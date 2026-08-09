-- Datos de ejemplo (perfil "dev" con H2), calcados de los MOCK que hoy usa el frontend
-- en post.service.ts y chat.service.ts, para poder probar el feed y el chat sin
-- tener que registrar usuarios manualmente primero.
-- La contraseña de los 3 usuarios de ejemplo es: Password1

INSERT INTO usuarios (id, nombre_usuario, email, password_hash, foto_perfil_url, fecha_creacion) VALUES
  (1, 'admin_kiert', 'admin@kiert.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOe6D0KcTvVQyKG3q3F.68f1M6XPfEfyi', NULL, CURRENT_TIMESTAMP),
  (2, 'root_ana', 'ana@kiert.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOe6D0KcTvVQyKG3q3F.68f1M6XPfEfyi', NULL, CURRENT_TIMESTAMP),
  (3, 'kai_dev', 'kai@kiert.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOe6D0KcTvVQyKG3q3F.68f1M6XPfEfyi', NULL, CURRENT_TIMESTAMP);

INSERT INTO posts (id, autor_id, titulo, descripcion, categoria, fecha_creacion) VALUES
  (1, 2, 'Detecté un phishing dirigido a mi empresa',
      'Recibi un correo que suplantaba a mi banco pidiendo actualizar datos. Comparto los headers y como lo identifique.',
      'CASO_HACKING', CURRENT_TIMESTAMP),
  (2, 3, 'Perdi acceso a mi cuenta de correo, como recupero todo?',
      'Me cambiaron la contrasena sin avisarme. Ya reporte al proveedor pero no se que mas hacer mientras espero respuesta.',
      'AYUDA', CURRENT_TIMESTAMP);

INSERT INTO adjuntos (id, post_id, tipo, nombre, url, peso_kb) VALUES
  (1, 1, 'LINK', 'Analisis en VirusTotal', 'https://virustotal.com', NULL);

INSERT INTO comentarios (id, post_id, autor_id, contenido, fecha_creacion) VALUES
  (1, 1, 3, 'Te recomiendo tambien reportarlo a PhishTank, ayuda a que se bloquee mas rapido.', CURRENT_TIMESTAMP);
