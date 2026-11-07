const bcrypt = require('bcryptjs');
const words = ['admin123', '123456', 'student123'];
for (const w of words) {
  console.log(w + ' => ' + bcrypt.hashSync(w, 10));
}
