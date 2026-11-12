package co.edu.uniquindio.billeteradi.tests;

import co.edu.uniquindio.billeteradi.model.entity.Billetera;
import co.edu.uniquindio.billeteradi.model.entity.Usuario;
import co.edu.uniquindio.billeteradi.model.enums.EstadoBilletera;
import co.edu.uniquindio.billeteradi.model.enums.NivelUsuario;
import co.edu.uniquindio.billeteradi.model.enums.TipoBilletera;
import co.edu.uniquindio.billeteradi.model.exception.ReglaNegocioException;
import co.edu.uniquindio.billeteradi.model.repository.RepositorioMemoria;
import co.edu.uniquindio.billeteradi.model.service.UsuarioService;

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
        repositoryChecks();
        serviceChecks();
        System.out.println("OK: " + checks + " verificaciones pasaron.");
    }

    private static void repositoryChecks() {
        RepositorioMemoria repo = new RepositorioMemoria();
        Usuario first = new Usuario("U1", "Ana");
        check(repo.agregarUsuario(first), "agrega usuario");
        check(!repo.agregarUsuario(new Usuario("U1", "Beto")), "rechaza usuario duplicado");
        check(repo.totalUsuarios() == 1, "conserva un solo usuario");
        check(repo.buscarUsuario("X").isEmpty(), "usuario inexistente");
        check(repo.existeUsuario("U1"), "detecta usuario existente");
        check(repo.listarUsuarios().get(0) == first, "conserva orden de usuarios");
        expect(UnsupportedOperationException.class, () -> repo.listarUsuarios().clear(), "usuarios de solo lectura");

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

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) {
            throw new AssertionError("FALLÓ: " + description);
        }
    }

    private static void expect(Class<? extends Throwable> expected, Runnable action, String description) {
        checks++;
        try {
            action.run();
        } catch (Throwable actual) {
            if (expected.isInstance(actual)) {
                return;
            }
            throw new AssertionError("FALLÓ: " + description + " (se esperaba "
                    + expected.getSimpleName() + " pero ocurrió " + actual.getClass().getSimpleName() + ")", actual);
        }
        throw new AssertionError("FALLÓ: " + description + " (no se lanzó " + expected.getSimpleName() + ")");
    }
}
