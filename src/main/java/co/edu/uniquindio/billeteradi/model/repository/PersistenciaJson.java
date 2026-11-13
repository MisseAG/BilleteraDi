package co.edu.uniquindio.billeteradi.model.repository;

import co.edu.uniquindio.billeteradi.model.entity.Billetera;
import co.edu.uniquindio.billeteradi.model.entity.Transaccion;
import co.edu.uniquindio.billeteradi.model.entity.Usuario;
import co.edu.uniquindio.billeteradi.model.enums.EstadoBilletera;
import co.edu.uniquindio.billeteradi.model.enums.EstadoTransaccion;
import co.edu.uniquindio.billeteradi.model.enums.NivelUsuario;
import co.edu.uniquindio.billeteradi.model.enums.TipoBilletera;
import co.edu.uniquindio.billeteradi.model.enums.TipoTransaccion;
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
import java.util.Optional;

/**
 * Persistencia en formato JSON sin dependencias externas.
 */
public class PersistenciaJson {

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

                    String usuarioId = null;
                    if (origenId != null) {
                        Optional<Billetera> bOrigen = repo.buscarBilletera(origenId);
                        if (bOrigen.isPresent()) usuarioId = bOrigen.get().getPropietarioId();
                    } else if (destinoId != null) {
                        Optional<Billetera> bDestino = repo.buscarBilletera(destinoId);
                        if (bDestino.isPresent()) usuarioId = bDestino.get().getPropietarioId();
                    }
                    repo.registrarTransaccion(tx, usuarioId);
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
}
