package br.com.acme.cervejariaacme.service;

import br.com.acme.cervejariaacme.model.Cerveja;
import br.com.acme.cervejariaacme.model.Estilo;
import br.com.acme.cervejariaacme.model.Lupulo;
import br.com.acme.cervejariaacme.model.Marca;
import br.com.acme.cervejariaacme.model.Usuario;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioCervejasRuimPropertyTest {

    @Property
    void totalCervejas_deveRetornarTamanhoDaListaESemSideEffects(@ForAll("listaCervejas") List<Cerveja> cervejas) {
        RelatorioCervejasRuim relatorio = new RelatorioCervejasRuim();
        relatorio.resetar();
        List<Cerveja> entrada = new ArrayList<>(cervejas);
        int tamanhoOriginal = entrada.size();

        int total = relatorio.totalCervejas(entrada);

        assertEquals(tamanhoOriginal, total);
        assertEquals(tamanhoOriginal, entrada.size(), "lista não pode ser alterada");
    }

    @Property
    void porcentagemComLupulo_deveSerCaseInsensitiveEBetweenZeroAndHundred(
            @ForAll("listaCervejas") List<Cerveja> cervejas,
            @ForAll("nomesLupulo") String nomeLupulo
    ) {
        RelatorioCervejasRuim relatorio = new RelatorioCervejasRuim();
        relatorio.resetar();
        List<Cerveja> entrada = new ArrayList<>(cervejas);

        double porcentagem = relatorio.porcentagemComLupulo(entrada, nomeLupulo);

        double esperado = porcentagemEsperada(entrada, nomeLupulo);
        assertEquals(esperado, porcentagem, 0.0001);
        assertTrue(porcentagem >= 0.0 && porcentagem <= 100.0, "porcentagem deve ficar entre 0 e 100");
    }

    @Property
    void top3CervejasPorCurtidas_deveRetornarAteTresOrdenadasPorCurtidasEDepoisNome(
            @ForAll("listaCervejas") List<Cerveja> cervejas
    ) {
        RelatorioCervejasRuim relatorio = new RelatorioCervejasRuim();
        relatorio.resetar();
        List<Cerveja> entrada = new ArrayList<>(cervejas);

        List<String> resultado = relatorio.top3CervejasPorCurtidas(entrada);

        List<String> esperado = entrada.stream()
                .sorted(curtidasComparator().reversed())
                .limit(3)
                .map(Cerveja::getNome)
                .toList();

        assertEquals(esperado, resultado);
        assertTrue(resultado.size() <= 3);
    }

    @Property
    void contarPorMarca_deveAgruparPorNome(@ForAll("listaCervejas") List<Cerveja> cervejas) {
        RelatorioCervejasRuim relatorio = new RelatorioCervejasRuim();
        relatorio.resetar();
        List<Cerveja> entrada = new ArrayList<>(cervejas);

        Map<String, Integer> resultado = relatorio.contarPorMarca(entrada);

        Map<String, Long> esperado = entrada.stream()
                .collect(Collectors.groupingBy(c -> safeNomeMarca(c.getMarca()), Collectors.counting()));

        Map<String, Integer> esperadoComoInt = new HashMap<>();
        esperado.forEach((k, v) -> esperadoComoInt.put(k, v.intValue()));

        assertEquals(esperadoComoInt, resultado);
    }

    @Provide
    Arbitrary<List<Cerveja>> listaCervejas() {
        return Arbitraries.collections()
                .list(cervejaArbitrary())
                .ofMinSize(0)
                .ofMaxSize(8);
    }

    @Provide
    Arbitrary<String> nomesLupulo() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(0)
                .ofMaxLength(10)
                .map(s -> s + " ")
                .map(String::trim);
    }

    private Arbitrary<Cerveja> cervejaArbitrary() {
        Arbitrary<String> nomes = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(12);
        Arbitrary<String> marcas = Arbitraries.of("Acme", "Brau", "Citrus", "Delta", "Épica");
        Arbitrary<String> estilos = Arbitraries.of("IPA", "Pilsner", "Stout", "Lager", "Sour");
        Arbitrary<List<Lupulo>> lupulos = Arbitraries.collections()
                .list(lupuloArbitrary())
                .ofMinSize(0)
                .ofMaxSize(4);
        Arbitrary<Integer> curtidas = Arbitraries.integers().between(0, 5);

        return Arbitraries.combine(nomes, marcas, estilos, lupulos, curtidas)
                .as((nome, marca, estilo, listaLupulos, totalCurtidas) -> Cerveja.builder()
                        .nome(nome)
                        .marca(Marca.builder().nome(marca).pais("BR").build())
                        .estilo(Estilo.builder().nome(estilo).build())
                        .lupulos(listaLupulos)
                        .curtidas(usuarios(totalCurtidas))
                        .build());
    }

    private Arbitrary<Lupulo> lupuloArbitrary() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(10)
                .map(nome -> Lupulo.builder().nome(nome).build());
    }

    private Set<Usuario> usuarios(int quantidade) {
        return IntStream.range(0, quantidade)
                .mapToObj(i -> Usuario.builder()
                        .id((long) i + 1)
                        .nome("user-" + i)
                        .build())
                .collect(Collectors.toSet());
    }

    private double porcentagemEsperada(List<Cerveja> cervejas, String nomeLupulo) {
        if (cervejas == null || cervejas.isEmpty()) {
            return 0.0;
        }
        long com = cervejas.stream()
                .filter(c -> contemLupulo(c, nomeLupulo))
                .count();
        return (com * 100.0) / cervejas.size();
    }

    private boolean contemLupulo(Cerveja cerveja, String alvo) {
        if (cerveja == null || alvo == null) {
            return false;
        }
        return cerveja.getLupulos().stream()
                .filter(l -> l.getNome() != null)
                .anyMatch(l -> l.getNome().equalsIgnoreCase(alvo));
    }

    private Comparator<Cerveja> curtidasComparator() {
        return Comparator
                .comparingInt((Cerveja c) -> c.getCurtidas() == null ? 0 : c.getCurtidas().size())
                .thenComparing(c -> c.getNome() == null ? "" : c.getNome(), String.CASE_INSENSITIVE_ORDER);
    }

    private String safeNomeMarca(Marca marca) {
        return marca == null || marca.getNome() == null ? "" : marca.getNome();
    }
}
