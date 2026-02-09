module.exports = {
    secret: process.env.JWT_SECRET || 'clave_secreta',
    expiresIn:'1h'
};
