package mx.edu.tidesk.ti_desk.usuario;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import mx.edu.tidesk.ti_desk.usuario.dto.RegistroRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegistroRequestTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    private static final String CORREO_OK = "ana.lopez@unistmo.edu.mx";
    private static final String CLAVE_OK = "Segura#2026";

    private static Set<String> camposInvalidos(String nombre, String correo, String contrasena) {
        Set<ConstraintViolation<RegistroRequest>> violaciones =
                VALIDATOR.validate(new RegistroRequest(nombre, correo, contrasena));
        return violaciones.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    void datosValidosNoTienenErrores() {
        assertThat(camposInvalidos("Ana López", CORREO_OK, CLAVE_OK)).isEmpty();
    }

    @Test
    void elCorreoInstitucionalNoDistingueMayusculas() {
        assertThat(camposInvalidos("Ana", "Ana.Lopez@UNISTMO.EDU.MX", CLAVE_OK)).isEmpty();
    }

    @Test
    void nombreVacioEsInvalido() {
        assertThat(camposInvalidos("", CORREO_OK, CLAVE_OK)).containsExactly("nombre");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "correo-sin-arroba", "ana.lopez@gmail.com",
            "ana.lopez@unistmo.com", "ana.lopez@unistmo.edu.mx.co"})
    void correosNoInstitucionalesOMalFormadosSonInvalidos(String correo) {
        assertThat(camposInvalidos("Ana", correo, CLAVE_OK)).contains("correo");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "Corta#1", "sinmayuscula#1", "SINMINUSCULA#1",
            "SinNumero#abc", "SinSimbolo2026", "Contraseña#2026"})
    void contrasenasQueNoCumplenLaPoliticaSonInvalidas(String contrasena) {
        assertThat(camposInvalidos("Ana", CORREO_OK, contrasena)).contains("contrasena");
    }

    @Test
    void contrasenaDe72CaracteresEsValidaYDe73No() {
        String de72 = "Aa1#" + "x".repeat(68);
        String de73 = de72 + "x";

        assertThat(camposInvalidos("Ana", CORREO_OK, de72)).isEmpty();
        assertThat(camposInvalidos("Ana", CORREO_OK, de73)).contains("contrasena");
    }

    @Test
    void toStringNoMuestraLaContrasena() {
        String texto = new RegistroRequest("Ana", CORREO_OK, CLAVE_OK).toString();

        assertThat(texto).doesNotContain(CLAVE_OK);
    }
}