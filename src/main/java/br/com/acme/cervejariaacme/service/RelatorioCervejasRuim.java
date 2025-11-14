package br.com.acme.cervejariaacme.service;

import br.com.acme.cervejariaacme.model.Cerveja;
import br.com.acme.cervejariaacme.model.Estilo;
import br.com.acme.cervejariaacme.model.Lupulo;
import br.com.acme.cervejariaacme.model.Marca;
import br.com.acme.cervejariaacme.model.Usuario;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class RelatorioCervejasRuim {

    public static final Map<String, Object> STATIC_CACHE = new ConcurrentHashMap<>();
    public static final List<String> ULTIMO_TOP3 = Collections.synchronizedList(new ArrayList<>());
    public static final AtomicInteger CONTADOR = new AtomicInteger(0);
    public static String ULTIMA_MARCA_POP = null;

    private static final int LIMITE_CORTE_TOP = 3;

    public int totalCervejas(List<Cerveja> cervejas) {
        return cervejas == null ? 0 : cervejas.size();
    }

    public int totalCurtidas(List<Cerveja> cervejas) {
        if (cervejas == null) {
            return 0;
        }
        return cervejas.stream()
                .filter(Objects::nonNull)
                .map(Cerveja::getCurtidas)
                .mapToInt(set -> set == null ? 0 : set.size())
                .sum();
    }

    public double mediaCurtidasPorCerveja(List<Cerveja> cervejas) {
        if (cervejas == null || cervejas.isEmpty()) {
            return 0.0;
        }
        return totalCurtidas(cervejas) / (double) cervejas.size();
    }

    public String marcaMaisPopularPorCurtidas(List<Cerveja> cervejas) {
        if (cervejas == null || cervejas.isEmpty()) {
            ULTIMA_MARCA_POP = null;
            return null;
        }

        Map<String, MarcaAggregate> aggregations = new HashMap<>();
        for (Cerveja cerveja : cervejas) {
            if (cerveja == null || cerveja.getMarca() == null) {
                continue;
            }
            String nome = safeTrim(cerveja.getMarca().getNome());
            if (nome == null || nome.isBlank()) {
                continue;
            }
            String normalized = nome.toLowerCase(Locale.ROOT);
            int curtidas = cerveja.getCurtidas() == null ? 0 : cerveja.getCurtidas().size();
            aggregations.compute(normalized, (key, current) -> {
                if (current == null) {
                    return new MarcaAggregate(nome, curtidas);
                }
                current.increment(curtidas);
                current.updateDisplayName(nome);
                return current;
            });
        }

        String escolhida = aggregations.values().stream()
                .sorted(Comparator
                        .comparingInt(MarcaAggregate::getCurtidas).reversed()
                        .thenComparing(MarcaAggregate::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(MarcaAggregate::getNome)
                .findFirst()
                .orElse(null);

        ULTIMA_MARCA_POP = escolhida;
        return escolhida;
    }

    public String estiloComMaisRotulos(List<Cerveja> cervejas) {
        if (cervejas == null || cervejas.isEmpty()) {
            return null;
        }

        Map<String, Integer> contagem = new HashMap<>();
        for (Cerveja cerveja : cervejas) {
            Estilo estilo = cerveja == null ? null : cerveja.getEstilo();
            String nome = estilo == null ? null : safeTrim(estilo.getNome());
            if (nome == null || nome.isBlank()) {
                continue;
            }
            contagem.merge(nome, 1, Integer::sum);
        }

        return contagem.entrySet().stream()
                .sorted(Comparator
                        .comparingInt(Map.Entry<String, Integer>::getValue).reversed()
                        .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public double porcentagemComLupulo(List<Cerveja> cervejas, String nomeLupulo) {
        if (cervejas == null || cervejas.isEmpty()) {
            return 0.0;
        }
        List<Cerveja> validas = cervejas.stream().filter(Objects::nonNull).toList();
        if (validas.isEmpty()) {
            return 0.0;
        }
        String alvo = nomeLupulo == null ? "" : nomeLupulo.trim();

        long comLupulo = validas.stream()
                .filter(c -> contemLupulo(c, alvo))
                .count();

        return (comLupulo * 100.0) / validas.size();
    }

    public double mediaLupulosPorCerveja(List<Cerveja> cervejas) {
        if (cervejas == null || cervejas.isEmpty()) {
            return 0.0;
        }
        List<Cerveja> validas = cervejas.stream().filter(Objects::nonNull).toList();
        if (validas.isEmpty()) {
            return 0.0;
        }

        int totalLupulos = validas.stream()
                .map(Cerveja::getLupulos)
                .mapToInt(l -> l == null ? 0 : l.size())
                .sum();

        return totalLupulos / (double) validas.size();
    }

    public int totalUsuariosUnicosQueCurtiram(List<Cerveja> cervejas) {
        if (cervejas == null || cervejas.isEmpty()) {
            return 0;
        }
        Set<Long> ids = new HashSet<>();
        for (Cerveja cerveja : cervejas) {
            if (cerveja == null || cerveja.getCurtidas() == null) {
                continue;
            }
            for (Usuario usuario : cerveja.getCurtidas()) {
                if (usuario != null && usuario.getId() != null) {
                    ids.add(usuario.getId());
                }
            }
        }
        return ids.size();
    }

    public List<String> top3CervejasPorCurtidas(List<Cerveja> cervejas) {
        if (cervejas == null || cervejas.isEmpty()) {
            synchronized (ULTIMO_TOP3) {
                ULTIMO_TOP3.clear();
            }
            return Collections.emptyList();
        }

        List<Cerveja> copia = cervejas.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        copia.sort(Comparator
                .comparingInt((Cerveja c) -> c.getCurtidas() == null ? 0 : c.getCurtidas().size()).reversed()
                .thenComparing(c -> safeDefault(c.getNome()), String.CASE_INSENSITIVE_ORDER));

        List<String> resultado = copia.stream()
                .map(c -> safeDefault(c.getNome()))
                .limit(LIMITE_CORTE_TOP)
                .collect(Collectors.toList());

        synchronized (ULTIMO_TOP3) {
            ULTIMO_TOP3.clear();
            ULTIMO_TOP3.addAll(resultado);
        }
        return resultado;
    }

    public Map<String, Integer> contarPorMarca(List<Cerveja> cervejas) {
        Map<String, Integer> porMarca = new HashMap<>();
        if (cervejas == null) {
            return porMarca;
        }
        for (Cerveja cerveja : cervejas) {
            Marca marca = cerveja == null ? null : cerveja.getMarca();
            String nome = marca == null ? null : safeTrim(marca.getNome());
            if (nome == null || nome.isBlank()) {
                continue;
            }
            porMarca.merge(nome, 1, Integer::sum);
        }
        return porMarca;
    }

    public void resetar() {
        STATIC_CACHE.clear();
        synchronized (ULTIMO_TOP3) {
            ULTIMO_TOP3.clear();
        }
        CONTADOR.set(0);
        ULTIMA_MARCA_POP = null;
    }

    private boolean contemLupulo(Cerveja cerveja, String alvo) {
        if (cerveja.getLupulos() == null || alvo == null || alvo.isBlank()) {
            return false;
        }
        return cerveja.getLupulos().stream()
                .filter(Objects::nonNull)
                .map(Lupulo::getNome)
                .filter(Objects::nonNull)
                .anyMatch(nome -> nome.equalsIgnoreCase(alvo));
    }

    private String safeTrim(String valor) {
        return valor == null ? null : valor.trim();
    }

    private String safeDefault(String valor) {
        return valor == null ? "" : valor;
    }

    private static class MarcaAggregate {
        private String nome;
        private int curtidas;

        MarcaAggregate(String nome, int curtidas) {
            this.nome = nome;
            this.curtidas = curtidas;
        }

        String getNome() {
            return nome == null ? "" : nome;
        }

        int getCurtidas() {
            return curtidas;
        }

        void increment(int valor) {
            this.curtidas += valor;
        }

        void updateDisplayName(String candidato) {
            if (candidato == null || candidato.isBlank()) {
                return;
            }
            if (this.nome == null || candidato.compareToIgnoreCase(this.nome) < 0) {
                this.nome = candidato;
            }
        }
    }
}
