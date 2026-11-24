// Configuración del chatbot Cuac (Botpress). Es la que exporta Botpress para este bot; solo se añade
// «embeddedChatId» para que el chat ocupe la pestaña entera en vez de salir como burbuja flotante,
// y el color violeta de la app.
window.botpress.on("webchat:initialized", function () {
  try { window.botpress.open(); } catch (e) {}
});
window.botpress.init({
  "botId": "27d9d432-96b0-4660-a075-24156adcc2b6",
  "configuration": {
    "version": "v2",
    "embeddedChatId": "bp-embedded-webchat",
    "color": "#6C4CF0",
    "themeMode": "light",
    "botName": "Cuac",
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