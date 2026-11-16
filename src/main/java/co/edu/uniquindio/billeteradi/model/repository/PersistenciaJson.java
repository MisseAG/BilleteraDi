package co.edu.uniquindio.billeteradi.model.repository;

import co.edu.uniquindio.billeteradi.model.entity.Billetera;
import co.edu.uniquindio.billeteradi.model.entity.OperacionProgramada;
import co.edu.uniquindio.billeteradi.model.entity.Transaccion;
import co.edu.uniquindio.billeteradi.model.entity.Usuario;
import co.edu.uniquindio.billeteradi.model.enums.EstadoBilletera;
import co.edu.uniquindio.billeteradi.model.enums.EstadoProgramacion;
import co.edu.uniquindio.billeteradi.model.enums.EstadoTransaccion;
import co.edu.uniquindio.billeteradi.model.enums.NivelUsuario;
import co.edu.uniquindio.billeteradi.model.enums.TipoBilletera;
import co.edu.uniquindio.billeteradi.model.enums.TipoTransaccion;
import co.edu.uniquindio.billeteradi.model.service.AnaliticaService;
import co.edu.uniquindio.billeteradi.model.service.ProgramacionService;
import co.edu.uniquindio.billeteradi.model.service.UsuarioService;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistencia en formato JSON sin dependencias externas.
 */
public class PersistenciaJson {

    public PersistenciaJson() {
    }

    private static final String DATA_DIR = "data";
    private static final String FILE_NAME = "billeteradi-data.json";

    private Path getFilePath() {
        return Paths.get(DATA_DIR, FILE_NAME);
    }

    public void guardar(RepositorioMemoria repo) {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");

            // Usuarios
            sb.append("  \"usuarios\": [\n");
            List<Usuario> usuarios = repo.listarUsuarios();
            for (int i = 0; i < usuarios.size(); i++) {
                Usuario u = usuarios.get(i);
                sb.append("    {");
                sb.append("\"id\": ").append(jsonStr(u.getId())).append(", ");
                sb.append("\"nombre\": ").append(jsonStr(u.getNombre())).append(", ");
                sb.append("\"activo\": ").append(u.isActivo()).append(", ");
                sb.append("\"puntosAcumuladosNetos\": ").append(u.getPuntosAcumuladosNetos()).append(", ");
                sb.append("\"puntosDisponibles\": ").append(u.getPuntosDisponibles()).append(", ");
                sb.append("\"nivel\": ").append(jsonStr(u.getNivel().name()));
                sb.append("}");
                if (i < usuarios.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ],\n");

            // Billeteras
            sb.append("  \"billeteras\": [\n");
            List<Billetera> billeteras = repo.listarTodasBilleteras();
            for (int i = 0; i < billeteras.size(); i++) {
                Billetera b = billeteras.get(i);
                sb.append("    {");
                sb.append("\"id\": ").append(jsonStr(b.getId())).append(", ");
                sb.append("\"propietarioId\": ").append(jsonStr(b.getPropietarioId())).append(", ");
                sb.append("\"nombre\": ").append(jsonStr(b.getNombre())).append(", ");
                sb.append("\"tipo\": ").append(jsonStr(b.getTipo().name())).append(", ");
                sb.append("\"saldo\": ").append(jsonStr(b.getSaldo().toPlainString())).append(", ");
                sb.append("\"estado\": ").append(jsonStr(b.getEstado().name()));
                sb.append("}");
                if (i < billeteras.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ],\n");

            // Transacciones
            sb.append("  \"transacciones\": [\n");
            List<Transaccion> txs = repo.listarTodasTransacciones();
            for (int i = 0; i < txs.size(); i++) {
                Transaccion tx = txs.get(i);
                sb.append("    {");
                sb.append("\"id\": ").append(jsonStr(tx.getId())).append(", ");
                sb.append("\"fecha\": ").append(jsonStr(tx.getFecha().toString())).append(", ");
                sb.append("\"tipo\": ").append(jsonStr(tx.getTipo().name())).append(", ");
                sb.append("\"importe\": ").append(jsonStr(tx.getImporte().toPlainString())).append(", ");
                sb.append("\"origenId\": ").append(tx.getOrigenId() != null ? jsonStr(tx.getOrigenId()) : "null").append(", ");
                sb.append("\"destinoId\": ").append(tx.getDestinoId() != null ? jsonStr(tx.getDestinoId()) : "null").append(", ");
                sb.append("\"estado\": ").append(jsonStr(tx.getEstado().name())).append(", ");
                sb.append("\"puntosGenerados\": ").append(tx.getPuntosGenerados()).append(", ");
                sb.append("\"transaccionOriginalId\": ").append(tx.getTransaccionOriginalId() != null ? jsonStr(tx.getTransaccionOriginalId()) : "null");
                sb.append("}");
                if (i < txs.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ],\n");

            // Consecutivos
            sb.append("  \"consecutivoBilletera\": ").append(repo.getConsecutivoBilletera()).append(",\n");
            sb.append("  \"consecutivoTransaccion\": ").append(repo.getConsecutivoTransaccion()).append("\n");

            sb.append("}\n");

            Files.writeString(getFilePath(), sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public void guardar(RepositorioMemoria repo, ProgramacionService programacionService) {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");

            List<Usuario> usuarios = repo.listarUsuarios();
            sb.append("  \"usuarios\": [\n");
            for (int i = 0; i < usuarios.size(); i++) {
                Usuario u = usuarios.get(i);
                sb.append("    {");
                sb.append("\"id\": ").append(jsonStr(u.getId())).append(", ");
                sb.append("\"nombre\": ").append(jsonStr(u.getNombre())).append(", ");
                sb.append("\"activo\": ").append(u.isActivo()).append(", ");
                sb.append("\"puntosAcumuladosNetos\": ").append(u.getPuntosAcumuladosNetos()).append(", ");
                sb.append("\"puntosDisponibles\": ").append(u.getPuntosDisponibles()).append(", ");
                sb.append("\"nivel\": ").append(jsonStr(u.getNivel().name()));
                sb.append("}");
                if (i < usuarios.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ],\n");

            List<Billetera> billeteras = repo.listarTodasBilleteras();
            sb.append("  \"billeteras\": [\n");
            for (int i = 0; i < billeteras.size(); i++) {
                Billetera b = billeteras.get(i);
                sb.append("    {");
                sb.append("\"id\": ").append(jsonStr(b.getId())).append(", ");
                sb.append("\"propietarioId\": ").append(jsonStr(b.getPropietarioId())).append(", ");
                sb.append("\"nombre\": ").append(jsonStr(b.getNombre())).append(", ");
                sb.append("\"tipo\": ").append(jsonStr(b.getTipo().name())).append(", ");
                sb.append("\"saldo\": ").append(jsonStr(b.getSaldo().toPlainString())).append(", ");
                sb.append("\"estado\": ").append(jsonStr(b.getEstado().name()));
                sb.append("}");
                if (i < billeteras.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ],\n");

            List<Transaccion> txs = repo.listarTodasTransacciones();
            sb.append("  \"transacciones\": [\n");
            for (int i = 0; i < txs.size(); i++) {
                Transaccion tx = txs.get(i);
                sb.append("    {");
                sb.append("\"id\": ").append(jsonStr(tx.getId())).append(", ");
                sb.append("\"fecha\": ").append(jsonStr(tx.getFecha().toString())).append(", ");
                sb.append("\"tipo\": ").append(jsonStr(tx.getTipo().name())).append(", ");
                sb.append("\"importe\": ").append(jsonStr(tx.getImporte().toPlainString())).append(", ");
                sb.append("\"origenId\": ").append(tx.getOrigenId() != null ? jsonStr(tx.getOrigenId()) : "null").append(", ");
                sb.append("\"destinoId\": ").append(tx.getDestinoId() != null ? jsonStr(tx.getDestinoId()) : "null").append(", ");
                sb.append("\"estado\": ").append(jsonStr(tx.getEstado().name())).append(", ");
                sb.append("\"puntosGenerados\": ").append(tx.getPuntosGenerados()).append(", ");
                sb.append("\"transaccionOriginalId\": ").append(tx.getTransaccionOriginalId() != null ? jsonStr(tx.getTransaccionOriginalId()) : "null");
                sb.append("}");
                if (i < txs.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ],\n");

            sb.append("  \"consecutivoBilletera\": ").append(repo.getConsecutivoBilletera()).append(",\n");
            sb.append("  \"consecutivoTransaccion\": ").append(repo.getConsecutivoTransaccion()).append(",\n");

            if (programacionService != null) {
                List<OperacionProgramada> ops = programacionService.listarTodas();
                sb.append("  \"operacionesProgramadas\": [\n");
                for (int i = 0; i < ops.size(); i++) {
                    OperacionProgramada op = ops.get(i);
                    sb.append("    {");
                    sb.append("\"id\": ").append(jsonStr(op.getId())).append(", ");
                    sb.append("\"usuarioId\": ").append(jsonStr(op.getUsuarioId())).append(", ");
                    sb.append("\"origenId\": ").append(op.getOrigenId() != null ? jsonStr(op.getOrigenId()) : "null").append(", ");
                    sb.append("\"destinoId\": ").append(op.getDestinoId() != null ? jsonStr(op.getDestinoId()) : "null").append(", ");
                    sb.append("\"tipo\": ").append(jsonStr(op.getTipo().name())).append(", ");
                    sb.append("\"importe\": ").append(jsonStr(op.getImporte().toPlainString())).append(", ");
                    sb.append("\"fechaEjecucion\": ").append(jsonStr(op.getFechaEjecucion().toString())).append(", ");
                    sb.append("\"consecutivo\": ").append(op.getConsecutivo()).append(", ");
                    sb.append("\"estado\": ").append(jsonStr(op.getEstado().name()));
                    sb.append("}");
                    if (i < ops.size() - 1) sb.append(",");
                    sb.append("\n");
                }
                sb.append("  ],\n");
                sb.append("  \"consecutivoProgramacion\": ").append(programacionService.getConsecutivoProgramacion()).append("\n");
            } else {
                sb.append("  \"operacionesProgramadas\": [],\n");
                sb.append("  \"consecutivoProgramacion\": 0\n");
            }

            sb.append("}\n");
            Files.writeString(getFilePath(), sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public boolean cargar(RepositorioMemoria repo, UsuarioService usuarioService) {
        Path path = getFilePath();
        if (!Files.exists(path)) {
            return false;
        }
        try {
            String contenido = Files.readString(path, StandardCharsets.UTF_8);
            return parsearJson(contenido, repo);
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean cargar(RepositorioMemoria repo, UsuarioService usuarioService,
                          ProgramacionService programacionService, AnaliticaService analiticaService) {
        boolean resultado = cargar(repo, usuarioService);
        if (!resultado) return false;

        try {
            String contenido = Files.readString(getFilePath(), StandardCharsets.UTF_8);

            if (programacionService != null) {
                String seccionOps = extraerArray(contenido, "operacionesProgramadas");
                if (seccionOps != null) {
                    List<String> objetos = extraerObjetos(seccionOps);
                    for (String obj : objetos) {
                        String id = extraerValorStr(obj, "id");
                        String usuarioId = extraerValorStr(obj, "usuarioId");
                        String origenId = extraerValorStrNullable(obj, "origenId");
                        String destinoId = extraerValorStrNullable(obj, "destinoId");
                        String tipo = extraerValorStr(obj, "tipo");
                        String importe = extraerValorStr(obj, "importe");
                        String fechaEj = extraerValorStr(obj, "fechaEjecucion");
                        long consecutivo = extraerValorLong(obj, "consecutivo");
                        String estado = extraerValorStr(obj, "estado");

                        if (id == null || usuarioId == null || tipo == null
                                || importe == null || fechaEj == null || estado == null) continue;

                        OperacionProgramada op = new OperacionProgramada(
                                id, usuarioId, origenId, destinoId,
                                TipoTransaccion.valueOf(tipo),
                                new BigDecimal(importe),
                                LocalDateTime.parse(fechaEj),
                                consecutivo,
                                EstadoProgramacion.valueOf(estado)
                        );
                        programacionService.agregarDirecto(op);
                    }
                }
                long consProg = extraerValorLong(contenido, "consecutivoProgramacion");
                programacionService.setConsecutivoProgramacion(consProg);
            }

            if (analiticaService != null) {
                for (Transaccion tx : repo.listarTodasTransacciones()) {
                    analiticaService.registrarTransaccionEnMontos(tx);
                }
            }
        } catch (Exception ignored) {
        }

        return true;
    }

    private boolean parsearJson(String json, RepositorioMemoria repo) {
        try {
            // Usuarios
            String seccionUsuarios = extraerArray(json, "usuarios");
            if (seccionUsuarios != null) {
                List<String> objetos = extraerObjetos(seccionUsuarios);
                for (String obj : objetos) {
                    String id = extraerValorStr(obj, "id");
                    String nombre = extraerValorStr(obj, "nombre");
                    if (id == null || nombre == null) continue;

                    boolean activo = extraerValorBool(obj, "activo");
                    long puntosAcum = extraerValorLong(obj, "puntosAcumuladosNetos");
                    long puntosDisp = extraerValorLong(obj, "puntosDisponibles");
                    String nivel = extraerValorStr(obj, "nivel");

                    Usuario u = new Usuario(id, nombre);
                    u.setActivo(activo);
                    u.setPuntosAcumuladosNetos(puntosAcum);
                    u.setPuntosDisponibles(puntosDisp);
                    if (nivel != null) {
                        try {
                            u.setNivel(NivelUsuario.valueOf(nivel));
                        } catch (Exception ignored) {
                        }
                    }
                    repo.agregarUsuario(u);
                }
            }

            // Billeteras
            String seccionBilleteras = extraerArray(json, "billeteras");
            if (seccionBilleteras != null) {
                List<String> objetos = extraerObjetos(seccionBilleteras);
                for (String obj : objetos) {
                    String id = extraerValorStr(obj, "id");
                    String propietarioId = extraerValorStr(obj, "propietarioId");
                    String nombre = extraerValorStr(obj, "nombre");
                    String tipo = extraerValorStr(obj, "tipo");
                    String saldo = extraerValorStr(obj, "saldo");
                    String estado = extraerValorStr(obj, "estado");

                    if (id == null || propietarioId == null || tipo == null) continue;

                    TipoBilletera tipoBil = TipoBilletera.valueOf(tipo);
                    Billetera b = new Billetera(id, propietarioId, nombre != null ? nombre : "Billetera", tipoBil);
                    if (saldo != null) {
                        b.setSaldo(new BigDecimal(saldo));
                    }
                    if (estado != null) {
                        try {
                            b.setEstado(EstadoBilletera.valueOf(estado));
                        } catch (Exception ignored) {
                        }
                    }
                    repo.agregarBilleteraDirecto(b);
                }
            }

            // Transacciones
            String seccionTxs = extraerArray(json, "transacciones");
            if (seccionTxs != null) {
                List<String> objetos = extraerObjetos(seccionTxs);
                for (String obj : objetos) {
                    String id = extraerValorStr(obj, "id");
                    String fecha = extraerValorStr(obj, "fecha");
                    String tipo = extraerValorStr(obj, "tipo");
                    String importe = extraerValorStr(obj, "importe");
                    String origenId = extraerValorStrNullable(obj, "origenId");
                    String destinoId = extraerValorStrNullable(obj, "destinoId");
                    String estado = extraerValorStr(obj, "estado");
                    long puntosGen = extraerValorLong(obj, "puntosGenerados");
                    String txOriginalId = extraerValorStrNullable(obj, "transaccionOriginalId");

                    if (id == null || fecha == null || tipo == null || importe == null || estado == null) continue;

                    Transaccion tx = new Transaccion(
                            id,
                            LocalDateTime.parse(fecha),
                            TipoTransaccion.valueOf(tipo),
                            new BigDecimal(importe),
                            origenId,
                            destinoId,
                            EstadoTransaccion.valueOf(estado),
                            puntosGen,
                            txOriginalId
                    );

                    String usuarioId = resolverPropietarioTransaccion(repo, tx);
                    repo.registrarTransaccion(tx, usuarioId);

                    if (usuarioId != null && tx.getEstado() == EstadoTransaccion.EXITOSA && tx.getTipo() !=  TipoTransaccion.REVERSION) {
                        repo.obtenerPilaReversion(usuarioId).push(tx.getId());
                    }
                }
            }

            long consB = extraerValorLong(json, "consecutivoBilletera");
            long consT = extraerValorLong(json, "consecutivoTransaccion");
            repo.setConsecutivoBilletera(consB);
            repo.setConsecutivoTransaccion(consT);

            return repo.totalUsuarios() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String jsonStr(String valor) {
        if (valor == null) return "null";
        return "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String extraerArray(String json, String clave) {
        String buscar = "\"" + clave + "\"";
        int idxClave = json.indexOf(buscar);
        if (idxClave < 0) return null;
        int dosPuntos = json.indexOf(':', idxClave + buscar.length());
        if (dosPuntos < 0) return null;
        int abreCorchete = json.indexOf('[', dosPuntos + 1);
        if (abreCorchete < 0) return null;

        int depth = 0;
        int fin = -1;
        for (int i = abreCorchete; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    fin = i;
                    break;
                }
            }
        }
        if (fin < 0) return null;
        return json.substring(abreCorchete, fin + 1);
    }

    private List<String> extraerObjetos(String arrayJson) {
        List<String> resultado = new ArrayList<>();
        int depth = 0;
        int inicio = -1;
        for (int i = 0; i < arrayJson.length(); i++) {
            char c = arrayJson.charAt(i);
            if (c == '{') {
                if (depth == 0) inicio = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && inicio >= 0) {
                    resultado.add(arrayJson.substring(inicio, i + 1));
                    inicio = -1;
                }
            }
        }
        return resultado;
    }

    private String extraerValorStr(String obj, String clave) {
        String buscar = "\"" + clave + "\"";
        int idxClave = obj.indexOf(buscar);
        if (idxClave < 0) return null;
        int dosPuntos = obj.indexOf(':', idxClave + buscar.length());
        if (dosPuntos < 0) return null;
        int primerComilla = obj.indexOf('"', dosPuntos + 1);
        if (primerComilla < 0) return null;
        int segundaComilla = obj.indexOf('"', primerComilla + 1);
        if (segundaComilla < 0) return null;
        return obj.substring(primerComilla + 1, segundaComilla);
    }

    private String extraerValorStrNullable(String obj, String clave) {
        String buscar = "\"" + clave + "\"";
        int idxClave = obj.indexOf(buscar);
        if (idxClave < 0) return null;
        int dosPuntos = obj.indexOf(':', idxClave + buscar.length());
        if (dosPuntos < 0) return null;
        String resto = obj.substring(dosPuntos + 1).trim();
        if (resto.startsWith("null")) return null;
        int primerComilla = resto.indexOf('"');
        if (primerComilla < 0) return null;
        int segundaComilla = resto.indexOf('"', primerComilla + 1);
        if (segundaComilla < 0) return null;
        return resto.substring(primerComilla + 1, segundaComilla);
    }

    private boolean extraerValorBool(String obj, String clave) {
        String buscar = "\"" + clave + "\"";
        int idxClave = obj.indexOf(buscar);
        if (idxClave < 0) return false;
        int dosPuntos = obj.indexOf(':', idxClave + buscar.length());
        if (dosPuntos < 0) return false;
        String resto = obj.substring(dosPuntos + 1).trim();
        return resto.startsWith("true");
    }

    private long extraerValorLong(String json, String clave) {
        String buscar = "\"" + clave + "\"";
        int idxClave = json.indexOf(buscar);
        if (idxClave < 0) return 0;
        int dosPuntos = json.indexOf(':', idxClave + buscar.length());
        if (dosPuntos < 0) return 0;
        int i = dosPuntos + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        StringBuilder sb = new StringBuilder();
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '-' || (c >= '0' && c <= '9')) {
                sb.append(c);
                i++;
            } else {
                break;
            }
        }
        if (sb.length() == 0) return 0;
        try {
            return Long.parseLong(sb.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    private String resolverPropietarioTransaccion(RepositorioMemoria repo, Transaccion tx) {
        if (tx.getTipo() == TipoTransaccion.REVERSION && tx.getTransaccionOriginalId() != null) {
            Transaccion original = repo.buscarTransaccion(tx.getTransaccionOriginalId()).orElse(null);
            if (original != null) {
                return resolverPropietarioTransaccion(repo, original);
            }
        }
        if (tx.getOrigenId() != null) {
            return repo.buscarBilletera(tx.getOrigenId())
                    .map(Billetera::getPropietarioId)
                    .orElse(null);
        }
        if (tx.getDestinoId() != null) {
            return repo.buscarBilletera(tx.getDestinoId())
                    .map(Billetera::getPropietarioId)
                    .orElse(null);
        }
        return null;
    }

}
