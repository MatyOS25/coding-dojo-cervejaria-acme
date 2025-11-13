package br.com.acme.cervejariaacme.service;

import br.com.acme.cervejariaacme.model.Cerveja;
import br.com.acme.cervejariaacme.model.Estilo;
import br.com.acme.cervejariaacme.model.Lupulo;
import br.com.acme.cervejariaacme.model.Marca;
import br.com.acme.cervejariaacme.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class RelatorioCervejasRuimTest {

    @InjectMocks
    private RelatorioCervejasRuim relatorio;

    @BeforeEach
    void resetEstadoCompartilhado() {
        relatorio.resetar();
    }

    @Test
    void totalCervejas_naoAlteraListaQuandoVazia() {
        List<Cerveja> cervejas = new ArrayList<>();

        int total = relatorio.totalCervejas(cervejas);

        assertEquals(0, total);
        assertTrue(cervejas.isEmpty(), "lista não deveria ser modificada");
    }

    @Test
    void totalCurtidas_consideraTodasAsCervejas() {
        List<Cerveja> cervejas = List.of(
                cerveja("Hop One", curtidas(2)),
                cerveja("Hop Two", curtidas(3))
        );

        int totalCurtidas = relatorio.totalCurtidas(cervejas);

        assertEquals(5, totalCurtidas);
    }

    @Test
    void mediaCurtidasPorCerveja_listaVaziaRetornaZero() {
        double media = relatorio.mediaCurtidasPorCerveja(List.of());

        assertEquals(0.0, media);
    }

    @Test
    void marcaMaisPopularPorCurtidas_desempataPorNomeCaseInsensitive() {
        Marca alpha = Marca.builder().nome("Alpha").build();
        Marca beta = Marca.builder().nome("beta").build();
        List<Cerveja> cervejas = List.of(
                cervejaComMarca("A", alpha, curtidas(2)),
                cervejaComMarca("B", beta, curtidas(2))
        );

        String marca = relatorio.marcaMaisPopularPorCurtidas(cervejas);

        assertEquals("Alpha", marca);
    }

    @Test
    void estiloComMaisRotulos_retornaEstiloComMaiorContagem() {
        Estilo lager = Estilo.builder().nome("Lager").build();
        Estilo pilsner = Estilo.builder().nome("Pilsner").build();
        List<Cerveja> cervejas = List.of(
                cervejaComEstilo("Primeira", lager),
                cervejaComEstilo("Segunda", pilsner),
                cervejaComEstilo("Terceira", pilsner)
        );

        String estilo = relatorio.estiloComMaisRotulos(cervejas);

        assertEquals("Pilsner", estilo);
    }

    @Test
    void porcentagemComLupulo_comparaSemDiferenciarCase() {
        Lupulo citraMaisculo = Lupulo.builder().nome("CITRA").build();
        Lupulo mosaic = Lupulo.builder().nome("Mosaic").build();
        List<Cerveja> cervejas = List.of(
                cervejaComLupulos("IPA", citraMaisculo),
                cervejaComLupulos("Lager", mosaic)
        );

        double porcentagem = relatorio.porcentagemComLupulo(cervejas, "citra");

        assertEquals(50.0, porcentagem);
    }

    @Test
    void mediaLupulosPorCerveja_calculaSemEstourarParaListaComUmItem() {
        Lupulo citra = Lupulo.builder().nome("Citra").build();
        Lupulo simcoe = Lupulo.builder().nome("Simcoe").build();
        List<Cerveja> cervejas = List.of(cervejaComLupulos("Single", citra, simcoe));

        double media = relatorio.mediaLupulosPorCerveja(cervejas);

        assertEquals(2.0, media);
    }

    @Test
    void totalUsuariosUnicosQueCurtiram_consideraIdComoIdentidade() {
        Usuario mesmoIdUsuario1 = Usuario.builder().id(1L).nome("Primeiro").build();
        Usuario mesmoIdUsuario2 = Usuario.builder().id(1L).nome("Segundo").build();
        List<Cerveja> cervejas = List.of(
                cerveja("IPA", curtidas(Set.of(mesmoIdUsuario1))),
                cerveja("Stout", curtidas(Set.of(mesmoIdUsuario2)))
        );

        int total = relatorio.totalUsuariosUnicosQueCurtiram(cervejas);

        assertEquals(1, total);
    }

    @Test
    void top3CervejasPorCurtidas_listaMenorQueTresNaoExplodeEOrdenaDecrescente() {
        List<Cerveja> cervejas = new ArrayList<>(List.of(
                cerveja("IPA", curtidas(5)),
                cerveja("Stout", curtidas(1))
        ));

        List<String> top = relatorio.top3CervejasPorCurtidas(cervejas);

        assertIterableEquals(List.of("IPA", "Stout"), top);
    }

    @Test
    void contarPorMarca_utilizaNomeDaMarcaComoChave() {
        Marca acme = Marca.builder().nome("ACME").pais("BR").build();
        List<Cerveja> cervejas = List.of(
                cervejaComMarca("IPA", acme, curtidas(0)),
                cervejaComMarca("Lager", acme, curtidas(0))
        );

        Map<String, Integer> porMarca = relatorio.contarPorMarca(cervejas);

        assertTrue(porMarca.containsKey("ACME"));
        assertEquals(2, porMarca.get("ACME"));
    }

    @Test
    void resetar_limpaEstadoCompartilhado() {
        RelatorioCervejasRuim.STATIC_CACHE.put("qualquer", "valor");
        RelatorioCervejasRuim.ULTIMO_TOP3.add("IPA");
        RelatorioCervejasRuim.CONTADOR.incrementAndGet();
        RelatorioCervejasRuim.ULTIMA_MARCA_POP = "ACME";

        relatorio.resetar();

        assertTrue(RelatorioCervejasRuim.STATIC_CACHE.isEmpty());
        assertTrue(RelatorioCervejasRuim.ULTIMO_TOP3.isEmpty());
        assertEquals(0, RelatorioCervejasRuim.CONTADOR.get());
        assertNull(RelatorioCervejasRuim.ULTIMA_MARCA_POP);
    }

    private Cerveja cerveja(String nome, Set<Usuario> curtidas) {
        return Cerveja.builder()
                .nome(nome)
                .curtidas(curtidas)
                .build();
    }

    private Cerveja cervejaComMarca(String nome, Marca marca, Set<Usuario> curtidas) {
        return Cerveja.builder()
                .nome(nome)
                .marca(marca)
                .curtidas(curtidas)
                .build();
    }

    private Cerveja cervejaComEstilo(String nome, Estilo estilo) {
        return Cerveja.builder()
                .nome(nome)
                .estilo(estilo)
                .build();
    }

    private Cerveja cervejaComLupulos(String nome, Lupulo... lupulos) {
        return Cerveja.builder()
                .nome(nome)
                .lupulos(List.of(lupulos))
                .build();
    }

    private Set<Usuario> curtidas(int quantidade) {
        return IntStream.range(0, quantidade)
                .mapToObj(i -> Usuario.builder()
                        .id((long) i + 1)
                        .nome("usuario-" + i)
                        .build())
                .collect(Collectors.toSet());
    }

    private Set<Usuario> curtidas(Set<Usuario> usuarios) {
        return usuarios;
    }
}
