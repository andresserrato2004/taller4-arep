// Combined Web Framework Demo JavaScript

// Función para formulario GET con XMLHttpRequest
function loadGetMsg() {
  const nameVar = document.getElementById("name").value;
  const xhttp = new XMLHttpRequest();
  xhttp.onload = function() {
    document.getElementById("getrespmsg").innerText = this.responseText;
  };
  xhttp.open("GET", "/hello?name=" + encodeURIComponent(nameVar));
  xhttp.send();
}

// Función para formulario POST con fetch
function loadPostMsg(inp) {
  const nameVal = inp && inp.value ? inp.value : document.getElementById("postname").value;
  const url = "/hellopost?name=" + encodeURIComponent(nameVal);
  fetch(url, { method: "POST" })
    .then((res) => res.text())
    .then((txt) => {
      document.getElementById("postrespmsg").innerText = txt;
    })
    .catch((err) => {
      document.getElementById("postrespmsg").innerText = "Error: " + err;
    });
}

// Función para apagar el servidor
function shutdownServer() {
  const shutdownMsg = document.getElementById("shutdownmsg");
  
  // Confirmar acción
  if (!confirm("¿Estás seguro de que quieres apagar el servidor?")) {
    return;
  }
  
  shutdownMsg.innerHTML = "⏳ Enviando señal de apagado...";
  shutdownMsg.style.color = "#ffa500";
  
  fetch("/shutdown", { method: "GET" })
    .then((res) => res.text())
    .then((txt) => {
      shutdownMsg.innerHTML = "✅ " + txt;
      shutdownMsg.style.color = "#4ecdc4";
      
      // Mostrar mensaje adicional
      setTimeout(() => {
        shutdownMsg.innerHTML += "<br>🔌 El servidor se está apagando...";
      }, 1000);
    })
    .catch((err) => {
      shutdownMsg.innerHTML = "❌ Error al apagar el servidor: " + err;
      shutdownMsg.style.color = "#ff6b6b";
    });
}

// Framework Demo functionality
document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 Combined Web Framework Demo loaded!');
    
    // Add click handlers to API links
    const apiLinks = document.querySelectorAll('.endpoint a');
    apiLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            console.log('Calling API endpoint:', this.href);
        });
    });
    
    // Add interactive behavior to endpoints
    const endpoints = document.querySelectorAll('.endpoint');
    endpoints.forEach(endpoint => {
        endpoint.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-2px)';
            this.style.boxShadow = '0 5px 15px rgba(0, 0, 0, 0.1)';
            this.style.transition = 'all 0.2s ease';
        });
        
        endpoint.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0)';
            this.style.boxShadow = 'none';
        });
    });
    
    // Show current time
    const currentTime = new Date().toLocaleString();
    console.log('Page loaded at:', currentTime);
    
    // Add form validation
    const nameInputs = document.querySelectorAll('input[name="name"]');
    nameInputs.forEach(input => {
        input.addEventListener('input', function() {
            if (this.value.length < 2) {
                this.style.borderColor = '#ff6b6b';
            } else {
                this.style.borderColor = '#4ecdc4';
            }
        });
    });
});