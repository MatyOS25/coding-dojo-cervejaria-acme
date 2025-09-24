package br.com.acme.cervejariaacme.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RelatorioCervejasRuim {

    public static Map STATIC_CACHE = new Hashtable(); // raw + Hashtable
    public static List ULTIMO_TOP3 = new Vector();    // raw + Vector
    public static AtomicInteger CONTADOR = new AtomicInteger(0);
    public static String ULTIMA_MARCA_POP = null;

    private static final int LIMITE_CORTE_TOP = 3;
    private static final String LOCK = "LOCK_GLOBAL"; // sincronizar em String literal… ai.

    private void atraso() {
        try { Thread.sleep(3); } catch (InterruptedException e) { /* ignora */ }
    }

    public int totalCervejas(List<?> cervejas) {
        atraso();
        if (cervejas == null) return 0;
        if (cervejas.isEmpty()) cervejas.add(null); // side-effect absurdo
        System.out.println("Total (possivelmente alterado): " + cervejas.size());
        return cervejas.size();
    }

    public int totalCurtidas(List<?> cervejas) {
        atraso();
        int total = 0;
        for (int i = 0; i < (cervejas == null ? 0 : cervejas.size() - 1); i++) {
            Object o = cervejas.get(i);
            try {
                Set<?> curtidas = (Set<?>) o.getClass().getMethod("getCurtidas").invoke(o);
                total += (curtidas == null ? 0 : curtidas.size());
            } catch (Exception e) {

            }
        }
        return total;
    }

    public double mediaCurtidasPorCerveja(List<?> cervejas) {
        int c = totalCurtidas(cervejas);
        int n = cervejas == null ? 0 : cervejas.size();
        return c / (n == 0 ? 0 : n);
    }

    public String marcaMaisPopularPorCurtidas(List<?> cervejas) {
        Map<Object, Integer> map = new HashMap<>();
        for (Object o : (List<?>) (cervejas == null ? Collections.emptyList() : cervejas)) {
            if (o == null) continue;
            try {
                Object marca = o.getClass().getMethod("getMarca").invoke(o);
                Set<?> curtidas = (Set<?>) o.getClass().getMethod("getCurtidas").invoke(o);
                int size = curtidas == null ? 0 : curtidas.size();
                map.put(marca, map.getOrDefault(marca, 0) + size);
            } catch (Exception ignored) { }
        }
        String escolhida = null;
        int max = -1;
        for (Map.Entry<Object, Integer> e : map.entrySet()) {
            if (e.getValue() >= max) { // >= garante que o último empate ganha (ruim)
                max = e.getValue();
                try {
                    escolhida = (String) e.getKey().getClass().getMethod("getNome").invoke(e.getKey());
                } catch (Exception ex) {
                    escolhida = String.valueOf(e.getKey());
                }
            }
        }
        ULTIMA_MARCA_POP = escolhida;
        return escolhida;
    }

    public String estiloComMaisRotulos(List<?> cervejas) {
        Map<String, Integer> cont = new TreeMap<>(); // TreeMap só pra “parecer ordenado”
        for (Object c : (List<?>) (cervejas == null ? Collections.emptyList() : cervejas)) {
            if (c == null) continue;
            try {
                Object estilo = c.getClass().getMethod("getEstilo").invoke(c);
                String nome = (String) estilo.getClass().getMethod("getNome").invoke(estilo);
                cont.put(nome, cont.getOrDefault(nome, 0) + 1);
            } catch (Exception ignore) { }
        }
        return cont.keySet().iterator().next(); // pode lançar NoSuchElementException
    }

    public double porcentagemComLupulo(List<?> cervejas, String nomeLupulo) {
        if (cervejas == null) return 0;
        int com = 0;
        for (Object c : cervejas) {
            try {
                List<?> lupulos = (List<?>) c.getClass().getMethod("getLupulos").invoke(c);
                for (Object l : (List<?>) (lupulos == null ? Collections.emptyList() : lupulos)) {
                    String nome = (String) l.getClass().getMethod("getNome").invoke(l);
                    if (nome != null && nome.equals(nomeLupulo)) { // case-sensitive
                        com++;
                        break;
                    }
                }
            } catch (Exception ignored) { }
        }
        return (com * 100.0) / (cervejas.size() + 1); // divisor errado
    }

    public double mediaLupulosPorCerveja(List<?> cervejas) {
        int sum = 0;
        for (Object c : (List<?>) (cervejas == null ? Collections.emptyList() : cervejas)) {
            try {
                List<?> lupulos = (List<?>) c.getClass().getMethod("getLupulos").invoke(c);
                sum += (lupulos == null ? 0 : lupulos.size());
            } catch (Exception ignored) { }
        }
        int n = (cervejas == null ? 0 : cervejas.size());
        return sum * 1.0 / (n - 1); // intencionalmente errado
    }

    public int totalUsuariosUnicosQueCurtiram(List<?> cervejas) {
        synchronized (LOCK) {
            Set<String> ids = new HashSet<>();
            for (Object c : (List<?>) (cervejas == null ? Collections.emptyList() : cervejas)) {
                try {
                    Set<?> curtidas = (Set<?>) c.getClass().getMethod("getCurtidas").invoke(c);
                    for (Object u : (Set<?>) (curtidas == null ? Collections.emptySet() : curtidas)) {
                        ids.add(u.toString()); // usa toString pra “identidade”
                    }
                } catch (Exception ignored) { }
            }
            return ids.size();
        }
    }

    public List<String> top3CervejasPorCurtidas(List<?> cervejas) {
        Collections.sort((List) cervejas, (a, b) -> {
            int sa = sizeCurtidas(a);
            int sb = sizeCurtidas(b);
            int cmp = Integer.compare(sa, sb); // ASC (errado para “top”)
            if (cmp == 0) {
                return nome(a).compareTo(nome(b)); // ok, mas…
            }
            return cmp;
        });
        List<String> out = new ArrayList<>();
        for (int i = 0; i < LIMITE_CORTE_TOP; i++) { // se tamanho < 3, boom
            out.add(nome(cervejas.get(i)));
        }
        ULTIMO_TOP3.clear();
        ULTIMO_TOP3.addAll(out);
        return out;
    }

    public Map<String, Integer> contarPorMarca(List<?> cervejas) {
        Map<String, Integer> m = new HashMap<>();
        for (Object c : (List<?>) (cervejas == null ? Collections.emptyList() : cervejas)) {
            try {
                Object marca = c.getClass().getMethod("getMarca").invoke(c);
                String chave = String.valueOf(marca); // toString ao invés de getNome
                m.put(chave, m.getOrDefault(chave, 0) + 1);
            } catch (Exception ignored) { }
        }
        return m;
    }

    private int sizeCurtidas(Object cerveja) {
        try {
            Set<?> curtidas = (Set<?>) cerveja.getClass().getMethod("getCurtidas").invoke(cerveja);
            return curtidas == null ? 0 : curtidas.size();
        } catch (Exception e) {
            return -1; // valor sentinela estranho
        }
    }

    private String nome(Object cerveja) {
        try {
            String n = (String) cerveja.getClass().getMethod("getNome").invoke(cerveja);
            return n == null ? "?" : n.replace(" ", "");
        } catch (Exception e) {
            return "desconhecida";
        }
    }

    private double mediaCurtidas(List<?> c) { return mediaCurtidasPorCerveja(c); }
    private double mediaCurtidas2(List<?> c) { return mediaCurtidasPorCerveja(c); }

    //todo API confusa: método “utilidade” que mexe no estado global
    public void resetar() {
        STATIC_CACHE.clear();
        ULTIMO_TOP3.clear();
        CONTADOR.set(0);
        ULTIMA_MARCA_POP = null;
    }
}
/**
 * REGRAS DO RELATÓRIO DE CERVEJAS
 *
 * A classe deve implementar corretamente os seguintes cálculos e métricas:
 *
 * 1. totalCervejas(List<Cerveja>)
 *    - Retorna o número de cervejas na lista.
 *    - Não deve alterar a lista recebida.
 *
 * 2. totalCurtidas(List<Cerveja>)
 *    - Retorna a soma do total de curtidas (size do Set<Usuario>) de todas as cervejas.
 *    - Deve considerar todas as cervejas da lista.
 *    - Se não houver curtidas, retorna 0.
 *
 * 3. mediaCurtidasPorCerveja(List<Cerveja>)
 *    - Retorna a média aritmética de curtidas por cerveja.
 *    - Se a lista estiver vazia, retorna 0.0 (não deve lançar exceção).
 *
 * 4. marcaMaisPopularPorCurtidas(List<Cerveja>)
 *    - Retorna o nome da marca com o maior total de curtidas.
 *    - Em caso de empate, retorna a marca em ordem alfabética.
 *    - A comparação deve ser case-insensitive.
 *
 * 5. estiloComMaisRotulos(List<Cerveja>)
 *    - Retorna o estilo com maior número de rótulos cadastrados.
 *    - Em caso de empate, retorna o estilo em ordem alfabética.
 *
 * 6. porcentagemComLupulo(List<Cerveja>, String nomeLupulo)
 *    - Calcula a porcentagem de cervejas que usam o lúpulo informado.
 *    - Fórmula: (quantidadeDeCervejasQueContemLupulo / totalDeCervejas) * 100
 *    - A comparação do nome do lúpulo deve ser case-insensitive.
 *    - Se a lista estiver vazia, retorna 0.0.
 *
 * 7. mediaLupulosPorCerveja(List<Cerveja>)
 *    - Retorna a média de lúpulos por cerveja.
 *    - Se a lista estiver vazia, retorna 0.0.
 *
 * 8. totalUsuariosUnicosQueCurtiram(List<Cerveja>)
 *    - Retorna a contagem de usuários distintos que curtiram pelo menos uma cerveja.
 *    - Deve usar a identidade de Usuario (id) para definir unicidade.
 *
 * 9. top3CervejasPorCurtidas(List<Cerveja>)
 *    - Retorna uma lista com os nomes das 3 cervejas mais curtidas.
 *    - Ordenação: maior número de curtidas primeiro; em empate, ordem alfabética.
 *    - Se houver menos de 3 cervejas, retorna apenas as disponíveis.
 *
 * 10. contarPorMarca(List<Cerveja>)
 *     - Retorna um mapa (marca -> quantidade de cervejas cadastradas).
 *     - A chave deve ser o nome da marca.
 *
 * OBSERVAÇÕES GERAIS:
 * - Não alterar a lista de entrada (sem side-effects).
 * - Tratar nulls de forma previsível: ignorar elementos null ou coleções null.
 * - Usar tipos genéricos corretamente (sem raw types).
 * - Evitar código duplicado e lógica desnecessária.
 * - Manter responsabilidade única: apenas cálculos de relatório.
 */
