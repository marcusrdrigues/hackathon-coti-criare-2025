package com.gestao.services;

import com.gestao.entities.Perfil;
import com.gestao.exceptions.DuplicateResourceException;
import com.gestao.exceptions.ResourceNotFoundException;
import com.gestao.repositories.PerfilRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PerfilService {

    private final PerfilRepository perfilRepository;

    @Transactional
    public Perfil criarPerfil(Perfil perfil) {
        if (perfilRepository.existsByNome(perfil.getNome())) {
            throw new DuplicateResourceException("Perfil já existe com esse nome!");
        }
        return perfilRepository.save(perfil);
    }

    @Transactional(readOnly = true)
    public Perfil buscarPorNome(String nome) {
        return perfilRepository.findByNome(nome)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado!"));
    }

    @Transactional(readOnly = true)
    public Perfil buscarPorId(UUID id) {
        return perfilRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado!"));
    }

    @Transactional(readOnly = true)
    public List<Perfil> listarTodos() {
        return perfilRepository.findAll();
    }
}
