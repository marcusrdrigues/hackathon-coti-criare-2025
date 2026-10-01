package com.gestao.services;

import com.gestao.entities.Perfil;
import com.gestao.repositories.PerfilRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PerfilService {

    private final PerfilRepository perfilRepository;

    // Criar perfil
    public Perfil criarPerfil(Perfil perfil) {
        if (perfilRepository.existsByNome(perfil.getNome())) {
            throw new RuntimeException("Perfil já existe com esse nome!");
        }
        return perfilRepository.save(perfil);
    }

    // Buscar perfil por nome
    public Perfil buscarPorNome(String nome) {
        return perfilRepository.findByNome(nome)
                .orElseThrow(() -> new RuntimeException("Perfil não encontrado!"));
    }

    // Buscar perfil por ID
    public Perfil buscarPorId(UUID id) {
        return perfilRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Perfil não encontrado!"));
    }

    // Listar todos os perfis
    public List<Perfil> listarTodos() {
        return perfilRepository.findAll();
    }
}