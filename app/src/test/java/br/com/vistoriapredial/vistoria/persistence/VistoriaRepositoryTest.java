package br.com.vistoriapredial.vistoria.persistence;

import br.com.vistoriapredial.usuario.domain.PerfilEnum;
import br.com.vistoriapredial.usuario.domain.Usuario;
import br.com.vistoriapredial.usuario.persistence.UsuarioRepository;
import br.com.vistoriapredial.vistoria.domain.ImagemVistoria;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import br.com.vistoriapredial.vistoria.domain.VistoriaStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class VistoriaRepositoryTest {

    @Autowired
    private VistoriaRepository vistoriaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void shouldSaveVistoriaWithImagens() {
        Usuario cliente = new Usuario("Test Client", "client@test.com", "password", PerfilEnum.ROLE_CLIENTE, null);
        usuarioRepository.save(cliente);

        Vistoria vistoria = new Vistoria();
        vistoria.setCliente(cliente);
        
        ImagemVistoria img = new ImagemVistoria();
        img.setUrl("http://local/img.jpg");
        img.setProtocoloItem("SALA");
        img.setVistoria(vistoria);
        
        vistoria.getImagens().add(img);

        Vistoria saved = vistoriaRepository.save(vistoria);

        assertNotNull(saved.getId());
        assertEquals(VistoriaStatus.EM_RASCUNHO, saved.getStatus());
        assertEquals(1, saved.getImagens().size());
        assertNotNull(saved.getImagens().get(0).getId());
    }
}
