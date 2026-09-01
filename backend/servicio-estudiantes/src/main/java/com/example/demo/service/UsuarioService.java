package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;

/**
 * UsuarioService — CRUD de usuarios.
 * 
 * La autenticación (UserDetailsService) la maneja CustomUserDetailsService.
 * Este servicio NO debe implementar UserDetailsService para evitar
 * conflicto de beans duplicados que causa StackOverflowError.
 */
@Service
public class UsuarioService {

	private final UsuarioRepository repo;
	private final PasswordEncoder encoder;

	public UsuarioService(UsuarioRepository repo, PasswordEncoder encoder) {
		this.repo = repo;
		this.encoder = encoder;
	}

	public List<Usuario> listarTodos() {
		return repo.findAll();
	}

	public Usuario buscarPorId(Long id) {
		return repo.findById(id).orElseThrow(() -> new RuntimeException("No encontrado"));
	}

	public Optional<Usuario> buscarPorUsername(String u) {
		return repo.findByUsername(u);
	}

	public void guardar(Usuario u) {
		u.setPassword(encoder.encode(u.getPassword()));
		repo.save(u);
	}

	public void eliminar(Long id) {
		repo.deleteById(id);
	}

	public boolean existeUsername(String u) {
		return repo.existsByUsername(u);
	}
}