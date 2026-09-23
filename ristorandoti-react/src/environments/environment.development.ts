export const environment = {
  production: false,
  // Base URL delle API Spring Boot in sviluppo
  apiUrl: 'http://localhost:8080/api',
  // Client ID OAuth di Google (Google Cloud Console → Credenziali → "Applicazione web").
  // Deve coincidere con app.google.client-id del backend. Se vuoto il pulsante Google è disabilitato.
  googleClientId: '346402964865-jqfj48mnsqctfjia9ic859gr1glvcgqt.apps.googleusercontent.com',
};
