package mx.edu.tidesk.ti_desk.usuario;

import jakarta.validation.Valid;
import java.util.List;
import mx.edu.tidesk.ti_desk.seguridad.UsuarioAutenticado;
import mx.edu.tidesk.ti_desk.usuario.dto.ActualizarUsuarioRequest;
import mx.edu.tidesk.ti_desk.usuario.dto.UsuarioResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Perfil propio y administración de usuarios (RF-03).
 * Solo recibe y responde; las reglas viven en UsuarioService (ADR-002).
 */
@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Perfil del usuario autenticado. El id sale del token, nunca de la petición. */
    @GetMapping("/me")
    public UsuarioResponse miPerfil(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return usuarioService.obtener(usuario.id());
    }

    /** RF-03: lista de usuarios, solo para ADMIN. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    /** RF-03: cambiar rol o activar/desactivar, solo para ADMIN. */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse actualizar(@PathVariable Long id,
                                      @Valid @RequestBody ActualizarUsuarioRequest cambios,
                                      @AuthenticationPrincipal UsuarioAutenticado administrador) {
        return usuarioService.actualizar(id, cambios, administrador.id());
    }
}