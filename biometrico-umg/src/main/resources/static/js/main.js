/* ══════════════════════════════════════════
   SISTEMA BIOMÉTRICO UMG — JavaScript Global
   ══════════════════════════════════════════ */

document.addEventListener('DOMContentLoaded', () => {

  const sidebarToggle = document.getElementById('sidebarToggle');
  const sidebar = document.getElementById('sidebar');
  const mainContent = document.querySelector('.main-content');
  const topbar = document.querySelector('.topbar');

  if (sidebarToggle && sidebar) {
    sidebarToggle.addEventListener('click', () => {
      const esMovil = window.innerWidth <= 768;

      if (esMovil) {
        sidebar.classList.remove('sidebar-hidden');
        sidebar.classList.toggle('open');
      } else {
        sidebar.classList.remove('open');
        sidebar.classList.toggle('sidebar-hidden');

        if (mainContent) mainContent.classList.toggle('main-expanded');
        if (topbar) topbar.classList.toggle('topbar-expanded');
      }
    });
  }

  document.querySelectorAll('.alert-auto').forEach(el => {
    setTimeout(() => {
      el.style.transition = 'opacity .6s ease';
      el.style.opacity = '0';
      setTimeout(() => el.remove(), 600);
    }, 4000);
  });

  document.querySelectorAll('[data-confirm]').forEach(btn => {
    btn.addEventListener('click', e => {
      if (!confirm(btn.dataset.confirm)) {
        e.preventDefault();
      }
    });
  });

  const videoEl = document.getElementById('webcamVideo');
  const canvasEl = document.getElementById('webcamCanvas');
  const captureBtn = document.getElementById('btnCapturar');
  const fotoInput = document.getElementById('fotoBase64');
  const previewImg = document.getElementById('previewFoto');

  if (videoEl && captureBtn) {
    navigator.mediaDevices.getUserMedia({ video: true })
        .then(stream => {
          videoEl.srcObject = stream;
          videoEl.play();
        })
        .catch(() => {
          console.warn('Cámara no disponible');
        });

    captureBtn.addEventListener('click', () => {
      if (!canvasEl || !videoEl) return;

      canvasEl.width = videoEl.videoWidth || 320;
      canvasEl.height = videoEl.videoHeight || 240;

      const contexto = canvasEl.getContext('2d');
      if (!contexto) return;

      contexto.drawImage(videoEl, 0, 0, canvasEl.width, canvasEl.height);

      const dataUrl = canvasEl.toDataURL('image/jpeg', 0.85);

      if (fotoInput) fotoInput.value = dataUrl;

      if (previewImg) {
        previewImg.src = dataUrl;
        previewImg.style.display = 'block';
      }
    });
  }

  const selInstalacion = document.getElementById('selInstalacion');
  const selPuerta = document.getElementById('selPuerta');

  if (selInstalacion && selPuerta) {
    selInstalacion.addEventListener('change', () => {
      const instalacionId = selInstalacion.value;
      if (!instalacionId) return;

      fetch(`/instalaciones/${instalacionId}/puertas-json`)
          .then(r => r.json())
          .then(puertas => {
            selPuerta.innerHTML = '<option value="">-- Seleccione --</option>';

            puertas.forEach(p => {
              const opt = document.createElement('option');
              opt.value = p.id;
              opt.textContent = p.nombre;
              selPuerta.appendChild(opt);
            });
          })
          .catch(error => {
            console.error('Error cargando puertas:', error);
          });
    });
  }

  document.querySelectorAll('.attendance-node').forEach(node => {
    node.addEventListener('click', () => {
      const checkbox = node.querySelector('input[type="checkbox"]');

      if (checkbox) {
        checkbox.checked = !checkbox.checked;
        node.classList.toggle('presente', checkbox.checked);
        node.classList.toggle('ausente', !checkbox.checked);

        const icon = node.querySelector('.node-status');
        if (icon) {
          icon.textContent = checkbox.checked ? '✓' : '✗';
        }
      }
    });
  });

  const menuGroups = document.querySelectorAll('.menu-group');
  const toggles = document.querySelectorAll('.menu-toggle');

  function cerrarMenus() {
    menuGroups.forEach(group => {
      group.classList.remove('open');
    });
  }

  toggles.forEach(toggle => {

    toggle.addEventListener('click', function () {

      const grupo = this.closest('.menu-group');
      const abierto = grupo.classList.contains('open');

      cerrarMenus();

      if (!abierto) {
        grupo.classList.add('open');
      }

    });

  });

// ===== MANTENER ABIERTO EL MENÚ ACTIVO =====

  const activeLink = document.querySelector('.submenu a.active');

  if (activeLink) {

    const activeGroup = activeLink.closest('.menu-group');

    if (activeGroup) {
      activeGroup.classList.add('open');
    }
  }

  document.addEventListener('click', function (e) {
    if (!sidebar) return;
    if (window.innerWidth > 768) return;

    const clicDentroSidebar = sidebar.contains(e.target);
    const clicEnBoton = sidebarToggle && sidebarToggle.contains(e.target);

    if (!clicDentroSidebar && !clicEnBoton) {
      sidebar.classList.remove('open');
    }
  });

});

document.addEventListener('DOMContentLoaded', function () {
  const loginMenuToggle = document.getElementById('loginMenuToggle');
  const loginArt = document.getElementById('loginArt');

  if (loginMenuToggle && loginArt) {
    loginMenuToggle.addEventListener('click', function () {
      loginArt.classList.toggle('open');
    });

    document.addEventListener('click', function (e) {
      const clicDentroPanel = loginArt.contains(e.target);
      const clicEnBoton = loginMenuToggle.contains(e.target);

      if (!clicDentroPanel && !clicEnBoton && window.innerWidth <= 900) {
        loginArt.classList.remove('open');
      }
    });
  }
});