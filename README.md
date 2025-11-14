## Cervejaria ACME E2E
Parte B do AT

### Requisitos
- Java 17+
- Maven 3.9+

### Como executar
1. Rode a aplicacao localmente usando mvn spring-boot:run.
2. Para os testes da parte b: `mvn -q -Dtest=RelatorioCervejasRuim* test`
3. E para verificar o relatorio Jacoco `mvn -q -Dtest=RelatorioCervejasRuim* verify`
