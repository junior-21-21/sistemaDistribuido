package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "usuarios")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "El username es obligatorio")
	@Column(unique = true, nullable = false)
	private String username;

	@NotBlank(message = "La password es obligatoria")
	@Column(nullable = false)
	private String password;

	@NotBlank(message = "El nombre es obligatorio")
	@Column(nullable = false)
	private String nombre;

	@NotBlank(message = "El apellido es obligatorio")
	@Column(nullable = false)
	private String apellido;

	@Column
	private String email;

	@NotBlank(message = "El rol es obligatorio")
	@Column(nullable = false)
	private String rol;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"))
	@Column(name = "rol", nullable = false, length = 50)
	private Set<String> roles = new LinkedHashSet<>();

	@Column(nullable = false)
	private String estado = "ACTIVO";

	@Column(name = "ultimo_acceso")
	private LocalDateTime ultimoAcceso;

	@Column(name = "debe_cambiar_clave")
	private Boolean debeCambiarClave = false;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "estudiante_id")
	private Estudiante estudiante;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "apoderado_id", unique = true)
	private Apoderado apoderado;

	public Usuario() {
	}

	public String getNombreCompleto() {
		return nombre + " " + apellido;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getApellido() {
		return apellido;
	}

	public void setApellido(String apellido) {
		this.apellido = apellido;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getRol() {
		return rol;
	}

	public void setRol(String rol) {
		this.rol = rol;
		if (rol != null && !rol.isBlank()) {
			if (roles == null) roles = new LinkedHashSet<>();
			roles.add(rol);
		}
	}

	public Set<String> getRoles() {
		if ((roles == null || roles.isEmpty()) && rol != null && !rol.isBlank()) {
			return Set.of(rol);
		}
		return roles;
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles == null ? new LinkedHashSet<>() : new LinkedHashSet<>(roles);
		if (!this.roles.isEmpty() && (rol == null || !this.roles.contains(rol))) {
			this.rol = this.roles.iterator().next();
		}
	}

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

	public LocalDateTime getUltimoAcceso() {
		return ultimoAcceso;
	}

	public void setUltimoAcceso(LocalDateTime ultimoAcceso) {
		this.ultimoAcceso = ultimoAcceso;
	}

	public Boolean getDebeCambiarClave() {
		return debeCambiarClave;
	}

	public void setDebeCambiarClave(Boolean debeCambiarClave) {
		this.debeCambiarClave = debeCambiarClave;
	}



	public Estudiante getEstudiante() {
		return estudiante;
	}

	public void setEstudiante(Estudiante estudiante) {
		this.estudiante = estudiante;
	}

	public Apoderado getApoderado() {
		return apoderado;
	}

	public void setApoderado(Apoderado apoderado) {
		this.apoderado = apoderado;
	}
}
