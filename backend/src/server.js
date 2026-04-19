require('dotenv').config();
const app = require('./app');
const { iniciarSchedulerPredicciones } = require('./jobs/prediccion.scheduler');

const PORT = process.env.PORT || 3000;

iniciarSchedulerPredicciones();

app.listen(PORT,()=>{
    console.log(`Servidor corriendo en puerto ${PORT}`);
});