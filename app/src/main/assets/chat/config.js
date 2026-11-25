// Configuración del chatbot Cuac (Botpress). Es la que exporta Botpress para este bot; solo se añade
// «embeddedChatId» para que el chat ocupe la pestaña entera en vez de salir como burbuja flotante,
// y el azul del logo de Fit-In.
window.botpress.on("webchat:initialized", function () {
  try { window.botpress.open(); } catch (e) {}
});

// Dentro de la app el chat ocupa la pestaña entera: se quita la X de cerrar de la cabecera.
// El webchat dibuja dentro de un «shadow DOM», así que el estilo se mete también ahí.
(function () {
  var css = ".bpHeaderContentActionsContainer > .bpHeaderContentActionsIcons:last-child{display:none!important}" +
    ".bpWebchat{border-radius:0!important;box-shadow:none!important}";
  function addTo(root) {
    if (!root || root.querySelector && root.querySelector("style[data-fitin]")) return;
    var st = document.createElement("style");
    st.setAttribute("data-fitin", "1");
    st.textContent = css;
    (root.head || root).appendChild(st);
  }
  function scan(node) {
    if (!node) return;
    if (node.shadowRoot) { addTo(node.shadowRoot); scan(node.shadowRoot); }
    var kids = node.children || [];
    for (var i = 0; i < kids.length; i++) scan(kids[i]);
  }
  addTo(document);
  var n = 0;
  var timer = setInterval(function () {
    scan(document.getElementById("bp-embedded-webchat"));
    if (++n > 40) clearInterval(timer); // 20 s
  }, 500);
})();
window.botpress.init({
  "botId": "27d9d432-96b0-4660-a075-24156adcc2b6",
  "configuration": {
    "version": "v2",
    "embeddedChatId": "bp-embedded-webchat",
    "color": "#145BCC",
    "themeMode": "light",
    "botName": "Cuac",
    "botAvatar": "https://appassets.androidplatform.net/assets/chat/cuac.png",
    "botDescription": "Resuelve dudas sobre cómo usar Fit-In con amabilidad y lenguaje sencillo.",
    "website": {},
    "email": {},
    "phone": {},
    "termsOfService": {},
    "privacyPolicy": {},
    "feedbackEnabled": true,
    "allowFileUpload": true,
    "soundEnabled": true,
    "conversationHistory": true,
    "homePageEnabled": true,
    "welcomeHeading": "¡Hola! Soy Cuac",
    "welcomeSubtitle": "Pregúntame cómo usar Fit-In. Si buscas actividades, pulsa el botón del pato en Inicio.",
    "conversationStartersEnabled": false,
    "conversationStarters": [],
    "conversationStartersDisplayStyle": "cards",
    "citationsEnabled": true
  },
  "clientId": "4981a8fb-9fb0-4a1e-9598-303814a9b99d"
});