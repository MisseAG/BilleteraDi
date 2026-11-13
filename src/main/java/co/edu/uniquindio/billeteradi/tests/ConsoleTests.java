package co.edu.uniquindio.billeteradi.tests;

import co.edu.uniquindio.billeteradi.model.entity.Billetera;
import co.edu.uniquindio.billeteradi.model.entity.Usuario;
import co.edu.uniquindio.billeteradi.model.enums.EstadoBilletera;
import co.edu.uniquindio.billeteradi.model.enums.NivelUsuario;
import co.edu.uniquindio.billeteradi.model.enums.TipoBilletera;
import co.edu.uniquindio.billeteradi.model.exception.ReglaNegocioException;
import co.edu.uniquindio.billeteradi.model.repository.PersistenciaJson;
import co.edu.uniquindio.billeteradi.model.repository.RepositorioMemoria;
import co.edu.uniquindio.billeteradi.model.service.UsuarioService;

import co.edu.uniquindio.billeteradi.model.enums.TipoTransaccion;
import co.edu.uniquindio.billeteradi.model.service.RecompensaService;
import co.edu.uniquindio.billeteradi.model.service.ResultadoPuntos;
import co.edu.uniquindio.billeteradi.model.structure.TablaNiveles;
import java.util.Map;

import java.math.BigDecimal;
import java.util.List;

/**
 * Verificaciones de consola para H2. Se ejecuta sin JUnit:
 * java --module-path target/classes -m co.edu.uniquindio.billeteradi/co.edu.uniquindio.billeteradi.tests.ConsoleTests
 */
public final class ConsoleTests {

    private static int checks;

    private ConsoleTests() {
    }

    public static void main(String[] args) {
        System.out.println("=== TESTS DE REPOSITORIO ===");
        repositoryChecks();
        System.out.println();
        System.out.println("=== TESTS DE SERVICIO ===");
        serviceChecks();
        System.out.println();
        System.out.println("=== TESTS DE PUNTOS Y NIVELES (RF09 y RF10) ===");
        rewardChecks();
        System.out.println();
        System.out.println("=== TESTS DE PERSISTENCIA ===");
        persistenciaChecks();
        System.out.println("OK: " + checks + " verificaciones pasaron.");
    }

    private static void persistenciaChecks() {
        RepositorioMemoria repo = new RepositorioMemoria();
        UsuarioService service = new UsuarioService(repo);
        PersistenciaJson persistencia = new PersistenciaJson();

        boolean cargado = persistencia.cargar(repo, service);
        check(cargado, "carga datos desde JSON");
        check(repo.totalUsuarios() >= 3, "carga al menos 3 usuarios desde JSON");
        check(repo.totalBilleteras() >= 4, "carga al menos 4 billeteras desde JSON");
        check(repo.existeUsuario("1001"), "usuario 1001 existe tras carga");
        Usuario ana = repo.buscarUsuario("1001").orElseThrow();
        check("Ana Garcia".equals(ana.getNombre()), "nombre de usuario 1001 es correcto");
        check(!repo.listarBilleterasDeUsuario("1001").isEmpty(), "usuario 1001 tiene billeteras asociadas");
    }

    private static void repositoryChecks() {
        System.out.println("-- Usuarios: altas, consultas y protección de datos --");
        RepositorioMemoria repo = new RepositorioMemoria();
        Usuario first = new Usuario("U1", "Ana");
        check(repo.agregarUsuario(first), "agrega usuario");
        check(!repo.agregarUsuario(new Usuario("U1", "Beto")), "rechaza usuario duplicado");
        check(repo.totalUsuarios() == 1, "conserva un solo usuario");
        check(repo.buscarUsuario("X").isEmpty(), "usuario inexistente");
        check(repo.existeUsuario("U1"), "detecta usuario existente");
        check(repo.listarUsuarios().get(0) == first, "conserva orden de usuarios");
        expect(UnsupportedOperationException.class, () -> repo.listarUsuarios().clear(), "usuarios de solo lectura");

        System.out.println("-- Billeteras: altas, duplicados e índices --");
        Billetera orphan = new Billetera("BIL-0001", "NOEXISTE", "Ahorro", TipoBilletera.AHORRO);
        expect(IllegalArgumentException.class, () -> repo.agregarBilletera(orphan), "rechaza billetera huérfana");
        check(repo.totalBilleteras() == 0, "no guarda billetera huérfana");

        check(repo.agregarBilletera(new Billetera("BIL-0001", "U1", "A", TipoBilletera.AHORRO)),
                "agrega billetera");
        check(!repo.agregarBilletera(new Billetera("BIL-0001", "U1", "B", TipoBilletera.COMPRAS)),
                "rechaza billetera duplicada");
        check(repo.totalBilleteras() == 1, "conserva una sola billetera");
        check(repo.listarBilleterasDeUsuario("U1").size() == 1, "índice de billeteras");
        expect(UnsupportedOperationException.class,
                () -> repo.listarBilleterasDeUsuario("U1").clear(), "billeteras de solo lectura");
        check("BIL-0001".equals(repo.siguienteCodigoBilletera()), "primer código consecutivo");
        check("BIL-0002".equals(repo.siguienteCodigoBilletera()), "segundo código consecutivo");
    }

    private static void serviceChecks() {
        RepositorioMemoria repo = new RepositorioMemoria();
        UsuarioService service = new UsuarioService(repo);

        System.out.println("-- Usuarios: registro y edición --");
        Usuario user = service.registrar(" U1 ", "Ana");
        check("U1".equals(user.getId()), "normaliza id");
        check(user.isActivo() && user.getPuntosAcumuladosNetos() == 0
                        && user.getPuntosDisponibles() == 0 && user.getNivel() == NivelUsuario.BRONCE,
                "usuario inicia activo y en bronce");
        expect(ReglaNegocioException.class, () -> service.registrar("U1", "Beto"), "rechaza id repetido");
        expect(ReglaNegocioException.class, () -> service.registrar(" ", "Ana"), "rechaza id vacío");
        expect(ReglaNegocioException.class, () -> service.registrar("U2", " "), "rechaza nombre vacío");

        check("Ana María".equals(service.editar("U1", "  Ana María  ").getNombre()), "edita nombre");
        expect(ReglaNegocioException.class, () -> service.editar("U9", "X"), "rechaza edición inexistente");
        expect(ReglaNegocioException.class, () -> service.editar("U1", " "), "rechaza nombre editado vacío");

        System.out.println("-- Billeteras: creación y datos iniciales --");
        Billetera savings = service.crearBilletera("U1", "Ahorro", TipoBilletera.AHORRO);
        Billetera expenses = service.crearBilletera("U1", "Gastos", TipoBilletera.GASTOS_DIARIOS);
        check(new BigDecimal("0.00").equals(savings.getSaldo()), "saldo inicial exacto");
        check(savings.getEstado() == EstadoBilletera.ACTIVA, "estado inicial activo");
        check("U1".equals(savings.getPropietarioId()), "propietario correcto");
        check(service.listarBilleteras("U1").equals(List.of(savings, expenses)), "índice y orden de billeteras");
        check("BIL-0001".equals(savings.getId()) && "BIL-0002".equals(expenses.getId()),
                "códigos de billetera");
        expect(ReglaNegocioException.class,
                () -> service.crearBilletera("U1", " ", TipoBilletera.AHORRO), "rechaza nombre de billetera");
        expect(ReglaNegocioException.class,
                () -> service.crearBilletera("U1", "Otra", null), "rechaza tipo nulo");
        expect(ReglaNegocioException.class,
                () -> service.crearBilletera("U9", "Otra", TipoBilletera.AHORRO), "rechaza usuario inexistente");

        System.out.println("-- Billeteras: cambios de estado --");
        service.cambiarEstadoBilletera(savings.getId(), EstadoBilletera.BLOQUEADA);
        check(savings.getEstado() == EstadoBilletera.BLOQUEADA, "bloquea billetera");
        service.cambiarEstadoBilletera(savings.getId(), EstadoBilletera.ACTIVA);
        check(savings.getEstado() == EstadoBilletera.ACTIVA, "reactiva billetera");
        expect(ReglaNegocioException.class,
                () -> service.cambiarEstadoBilletera(savings.getId(), EstadoBilletera.ACTIVA),
                "rechaza estado repetido");
        expect(ReglaNegocioException.class,
                () -> service.cambiarEstadoBilletera("BIL-9999", EstadoBilletera.BLOQUEADA),
                "rechaza billetera inexistente");
        expect(ReglaNegocioException.class,
                () -> service.cambiarEstadoBilletera(savings.getId(), null), "rechaza estado nulo");

        System.out.println("-- Usuarios: desactivación y búsquedas --");
        service.desactivar("U1");
        check(!service.obtenerUsuario("U1").isActivo(), "desactiva usuario");
        check(service.listarBilleteras("U1").size() == 2, "conserva billeteras al desactivar");
        expect(ReglaNegocioException.class, () -> service.desactivar("U1"), "rechaza desactivación repetida");
        expect(ReglaNegocioException.class,
                () -> service.crearBilletera("U1", "Nueva", TipoBilletera.COMPRAS),
                "rechaza billetera para usuario inactivo");
        expect(ReglaNegocioException.class, () -> service.listarBilleteras("U9"),
                "rechaza consulta de usuario inexistente");
        check(service.buscarUsuario(null).isEmpty() && service.buscarUsuario("U9").isEmpty(),
                "búsquedas vacías");
    }
    private static void rewardChecks() {
        RepositorioMemoria repo = new RepositorioMemoria();
        UsuarioService users = new UsuarioService(repo);
        RecompensaService rewards = new RecompensaService(repo);
        for (String id : List.of("U1", "U2", "U3", "U4", "U5", "U6", "U7", "U8")) {
            users.registrar(id, "Usuario " + id);
        }

        System.out.println("-- Puntos: p = k * floor(m / 100) --");
        Object[][] formula = {
                {TipoTransaccion.RECARGA, "1000.00", 10L},
                {TipoTransaccion.RETIRO, "1000.00", 20L},
                {TipoTransaccion.TRANSFERENCIA, "500.00", 15L},
                {TipoTransaccion.RECARGA, "100.00", 1L},
                {TipoTransaccion.RETIRO, "100.00", 2L},
                {TipoTransaccion.TRANSFERENCIA, "100.00", 3L},
                {TipoTransaccion.RECARGA, "99.99", 0L},
                {TipoTransaccion.TRANSFERENCIA, "99.99", 0L},
                {TipoTransaccion.RETIRO, "250.75", 4L},
                {TipoTransaccion.TRANSFERENCIA, "199.99", 3L},
                {TipoTransaccion.RECARGA, "0.01", 0L},
                {TipoTransaccion.RECARGA, "1234567.89", 12345L}
        };
        for (Object[] fila : formula) {
            TipoTransaccion tipo = (TipoTransaccion) fila[0];
            String importe = (String) fila[1];
            long esperado = (Long) fila[2];
            check(rewards.calcularPuntos(tipo, new BigDecimal(importe)) == esperado,
                    tipo + " de " + importe + " = " + esperado + " puntos");
        }

        System.out.println("-- Puntos: datos inválidos --");
        for (String malo : List.of("0", "0.00", "-1", "-100.00")) {
            expect(ReglaNegocioException.class,
                    () -> rewards.calcularPuntos(TipoTransaccion.RECARGA, new BigDecimal(malo)),
                    "rechaza importe " + malo);
        }
        expect(ReglaNegocioException.class,
                () -> rewards.calcularPuntos(TipoTransaccion.RECARGA, null), "rechaza importe nulo");
        expect(ReglaNegocioException.class,
                () -> rewards.calcularPuntos(null, new BigDecimal("100")), "rechaza tipo nulo");
        expect(ReglaNegocioException.class,
                () -> rewards.calcularPuntos(TipoTransaccion.REVERSION, new BigDecimal("500")),
                "una reversión no genera puntos");
        expect(ReglaNegocioException.class,
                () -> rewards.calcularPuntos(TipoTransaccion.TRANSFERENCIA, new BigDecimal("1e30")),
                "importe gigantesco -> mensaje, no ArithmeticException");

        System.out.println("-- Puntos: bono de pago programado --");
        check(rewards.getBonoProgramada() == 10, "bono por defecto = 10");
        check(rewards.calcularPuntos(TipoTransaccion.TRANSFERENCIA, new BigDecimal("500"), true) == 25,
                "programada exitosa: 15 base + 10 bono");
        check(rewards.calcularPuntos(TipoTransaccion.TRANSFERENCIA, new BigDecimal("500"), false) == 15,
                "no programada: sin bono");
        check(rewards.calcularPuntos(TipoTransaccion.RECARGA, new BigDecimal("50"), true) == 10,
                "el bono se suma aunque la base sea 0");
        rewards.setBonoProgramada(25);
        check(rewards.calcularPuntos(TipoTransaccion.RECARGA, new BigDecimal("100"), true) == 26,
                "bono configurable (25)");
        expect(ReglaNegocioException.class, () -> rewards.setBonoProgramada(-1), "rechaza bono negativo");
        check(rewards.getBonoProgramada() == 25, "el bono negativo no se aplicó");
        rewards.setBonoProgramada(RecompensaService.BONO_PROGRAMADA_POR_DEFECTO);

        System.out.println("-- Nivel: tabla de umbrales --");
        TablaNiveles table = new TablaNiveles();
        Object[][] limites = {
                {0L, NivelUsuario.BRONCE}, {500L, NivelUsuario.BRONCE},
                {501L, NivelUsuario.PLATA}, {1000L, NivelUsuario.PLATA},
                {1001L, NivelUsuario.ORO}, {5000L, NivelUsuario.ORO},
                {5001L, NivelUsuario.PLATINO}, {Long.MAX_VALUE, NivelUsuario.PLATINO}
        };
        for (Object[] fila : limites) {
            long puntos = (Long) fila[0];
            NivelUsuario esperado = (NivelUsuario) fila[1];
            check(table.nivelPara(puntos) == esperado, puntos + " puntos -> " + esperado);
        }
        expect(IllegalArgumentException.class, () -> table.nivelPara(-1), "tabla rechaza puntos negativos");
        expect(IllegalArgumentException.class,
                () -> new TablaNiveles(Map.of(100L, NivelUsuario.PLATA)),
                "tabla personalizada exige el nivel que empieza en 0");
        TablaNiveles custom = new TablaNiveles(Map.of(0L, NivelUsuario.BRONCE, 10L, NivelUsuario.ORO));
        check(custom.nivelPara(9) == NivelUsuario.BRONCE && custom.nivelPara(10) == NivelUsuario.ORO,
                "tabla personalizada usa sus umbrales");
        check(rewards.determinarNivel(501) == NivelUsuario.PLATA, "determinarNivel(501) = PLATA");
        expect(ReglaNegocioException.class, () -> rewards.determinarNivel(-1), "determinarNivel rechaza negativos");
        Usuario manual = users.obtenerUsuario("U7");
        manual.acreditarPuntos(1001);
        check(manual.getNivel() == NivelUsuario.BRONCE, "acreditar directo no recalcula el nivel");
        check(rewards.actualizarNivel(manual) == NivelUsuario.ORO && manual.getNivel() == NivelUsuario.ORO,
                "actualizarNivel guarda el nivel que corresponde");

        System.out.println("-- Acreditar puntos: guion de demostración --");
        ResultadoPuntos recarga = rewards.otorgarPorOperacion(
                "U1", TipoTransaccion.RECARGA, new BigDecimal("1000.00"), false);
        check(recarga.puntosOtorgados() == 10, "recarga de 1000.00 otorga 10 puntos");
        ResultadoPuntos transferencia = rewards.otorgarPorOperacion(
                "U1", TipoTransaccion.TRANSFERENCIA, new BigDecimal("500.00"), false);
        Usuario ana = users.obtenerUsuario("U1");
        check(transferencia.puntosOtorgados() == 15, "transferencia de 500.00 otorga 15 puntos");
        check(ana.getPuntosAcumuladosNetos() == 25 && ana.getPuntosDisponibles() == 25,
                "acumulados y disponibles = 25");
        check(ana.getNivel() == NivelUsuario.BRONCE && !transferencia.ascendio(), "sigue en Bronce, sin ascenso");
        check(users.obtenerUsuario("U2").getPuntosAcumuladosNetos() == 0,
                "solo recibe puntos el usuario indicado");

        System.out.println("-- Caso 13: cambio de nivel en los límites --");
        long[] deltas = {500, 1, 499, 1, 3999, 1};
        NivelUsuario[] niveles = {NivelUsuario.BRONCE, NivelUsuario.PLATA, NivelUsuario.PLATA,
                NivelUsuario.ORO, NivelUsuario.ORO, NivelUsuario.PLATINO};
        boolean[] ascensos = {false, true, false, true, false, true};
        long acumulado = 0;
        for (int i = 0; i < deltas.length; i++) {
            ResultadoPuntos r = rewards.otorgarPuntos("U3", deltas[i]);
            acumulado += deltas[i];
            check(r.nivelNuevo() == niveles[i] && r.ascendio() == ascensos[i]
                            && r.puntosAcumuladosNetos() == acumulado,
                    "con " + acumulado + " puntos -> " + niveles[i] + (ascensos[i] ? " (ascendió)" : ""));
        }
        check(users.obtenerUsuario("U3").getNivel() == NivelUsuario.PLATINO, "el nivel queda guardado en el usuario");
        ResultadoPuntos salto = rewards.otorgarPuntos("U4", 6000);
        check(salto.nivelAnterior() == NivelUsuario.BRONCE && salto.nivelNuevo() == NivelUsuario.PLATINO
                && salto.ascendio(), "un salto grande sube varios niveles");

        System.out.println("-- Acreditar puntos: casos límite y rechazos --");
        ResultadoPuntos cero = rewards.otorgarPorOperacion(
                "U5", TipoTransaccion.RECARGA, new BigDecimal("99.99"), false);
        check(cero.puntosOtorgados() == 0 && users.obtenerUsuario("U5").getPuntosAcumuladosNetos() == 0
                && !cero.ascendio(), "importe menor a 100: 0 puntos y sin cambios");
        rewards.otorgarPuntos("U5", 100);
        expect(ReglaNegocioException.class, () -> rewards.otorgarPuntos("U5", -5), "rechaza puntos negativos");
        expect(ReglaNegocioException.class,
                () -> rewards.otorgarPorOperacion("U5", TipoTransaccion.RECARGA, new BigDecimal("-10"), false),
                "rechaza operación con importe inválido");
        check(users.obtenerUsuario("U5").getPuntosAcumuladosNetos() == 100,
                "los rechazos no modificaron los puntos");
        expect(ReglaNegocioException.class, () -> rewards.otorgarPuntos("U9", 10), "rechaza usuario inexistente");
        expect(ReglaNegocioException.class, () -> rewards.otorgarPuntos(null, 10), "rechaza usuario nulo");

        rewards.otorgarPuntos("U6", Long.MAX_VALUE - 1);
        expect(ReglaNegocioException.class, () -> rewards.otorgarPuntos("U6", 5), "rechaza desbordar el máximo");
        Usuario cercano = users.obtenerUsuario("U6");
        check(cercano.getPuntosAcumuladosNetos() == Long.MAX_VALUE - 1
                && cercano.getPuntosDisponibles() == Long.MAX_VALUE - 1, "el desborde deja al usuario intacto");

        ResultadoPuntos programada = rewards.otorgarPorOperacion(
                "U8", TipoTransaccion.TRANSFERENCIA, new BigDecimal("500"), true);
        check(programada.puntosOtorgados() == 25, "pago programado exitoso acredita base + bono");
    }

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) {
            throw new AssertionError("FALLÓ: " + description);
        }
        System.out.println("  [OK] " + description);
    }

    private static void expect(Class<? extends Throwable> expected, Runnable action, String description) {
        checks++;
        try {
            action.run();
        } catch (Throwable actual) {
            if (expected.isInstance(actual)) {
                System.out.println("  [OK] " + description + " -> lanzó " + expected.getSimpleName());
                return;
            }
            throw new AssertionError("FALLÓ: " + description + " (se esperaba "
                    + expected.getSimpleName() + " pero ocurrió " + actual.getClass().getSimpleName() + ")", actual);
        }
        throw new AssertionError("FALLÓ: " + description + " (no se lanzó " + expected.getSimpleName() + ")");
    }
}
