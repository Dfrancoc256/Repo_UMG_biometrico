/* ══════════════════════════════════════════
   SISTEMA BIOMÉTRICO UMG — JavaScript Global
   ══════════════════════════════════════════ */

document.addEventListener('DOMContentLoaded', () => {

  // ── Sidebar toggle (mobile) ──────────────
  const sidebarToggle = document.getElementById('sidebarToggle');
  const sidebar = document.querySelector('.sidebar');
  if (sidebarToggle && sidebar) {
    sidebarToggle.addEventListener('click', () => sidebar.classList.toggle('open'));
  }

  // ── Auto-ocultar alertas ─────────────────
  document.querySelectorAll('.alert-auto').forEach(el => {
    setTimeout(() => { el.style.transition = 'opacity .6s'; el.style.opacity = '0';
      setTimeout(() => el.remove(), 600); }, 4000);
  });

  // ── Confirm modals via data-confirm ──────
  document.querySelectorAll('[data-confirm]').forEach(btn => {
    btn.addEventListener('click', e => {
      if (!confirm(btn.dataset.confirm)) e.preventDefault();
    });
  });

  // ── Webcam para captura de fotografía ────
  const videoEl = document.getElementById('webcamVideo');
  const canvasEl = document.getElementById('webcamCanvas');
  const captureBtn = document.getElementById('btnCapturar');
  const fotoInput = document.getElementById('fotoBase64');
  const previewImg = document.getElementById('previewFoto');

  if (videoEl && captureBtn) {
    navigator.mediaDevices.getUserMedia({ video: true })
      .then(stream => { videoEl.srcObject = stream; videoEl.play(); })
      .catch(() => console.warn('Cámara no disponible'));

    captureBtn.addEventListener('click', () => {
      if (!canvasEl || !videoEl) return;
      canvasEl.width  = videoEl.videoWidth  || 320;
      canvasEl.height = videoEl.videoHeight || 240;
      canvasEl.getContext('2d').drawImage(videoEl, 0, 0);
      const dataUrl = canvasEl.toDataURL('image/jpeg', .85);
      if (fotoInput)  fotoInput.value = dataUrl;
      if (previewImg) { previewImg.src = dataUrl; previewImg.style.display = 'block'; }
    });
  }

  // ── Filtro de instalación → puertas (reporte) ──
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
            opt.value = p.id; opt.textContent = p.nombre;
            selPuerta.appendChild(opt);
          });
        });
    });
  }

  // ── Árbol de asistencia: checkboxes ──────
  document.querySelectorAll('.attendance-node').forEach(node => {
    node.addEventListener('click', () => {
      const checkbox = node.querySelector('input[type="checkbox"]');
      if (checkbox) {
        checkbox.checked = !checkbox.checked;
        node.classList.toggle('presente', checkbox.checked);
        node.classList.toggle('ausente', !checkbox.checked);
        const icon = node.querySelector('.node-status');
        if (icon) icon.textContent = checkbox.checked ? '✓' : '✗';
      }
    });
  });
});
//sidebar tipo arbol
document.addEventListener("DOMContentLoaded", function () {
  const menuGroups = document.querySelectorAll(".menu-group");
  const toggles = document.querySelectorAll(".menu-toggle");
  const STORAGE_KEY = "sidebar-open-menu";

  function closeAllGroups() {
    menuGroups.forEach(group => group.classList.remove("active"));
  }

  function openGroup(group) {
    if (group) {
      group.classList.add("active");
    }
  }

  toggles.forEach(toggle => {
    toggle.addEventListener("click", function () {
      const currentGroup = this.parentElement;
      const menuName = this.dataset.menu;
      const isActive = currentGroup.classList.contains("active");

      closeAllGroups();

      if (!isActive) {
        openGroup(currentGroup);
        if (menuName) {
          localStorage.setItem(STORAGE_KEY, menuName);
        }
      } else {
        localStorage.removeItem(STORAGE_KEY);
      }
    });
  });

  const activeLink = document.querySelector(".submenu a.active");
  if (activeLink) {
    const parentGroup = activeLink.closest(".menu-group");
    openGroup(parentGroup);

    const activeToggle = parentGroup ? parentGroup.querySelector(".menu-toggle") : null;
    if (activeToggle && activeToggle.dataset.menu) {
      localStorage.setItem(STORAGE_KEY, activeToggle.dataset.menu);
    }
    return;
  }

  const savedMenu = localStorage.getItem(STORAGE_KEY);
  if (savedMenu) {
    const savedToggle = document.querySelector(`.menu-toggle[data-menu="${savedMenu}"]`);
    if (savedToggle) {
      openGroup(savedToggle.parentElement);
    }
  }
});
