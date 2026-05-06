const fs = require('fs');

function beautify(file) {
  if (!fs.existsSync(file)) return;
  let css = fs.readFileSync(file, 'utf8');

  // Fix footer links for light background
  css = css.replace(/color: rgba\(\s*255,\s*255,\s*255,\s*0\.6\s*\);/g, 'color: rgba(0, 0, 0, 0.45);');
  css = css.replace(/\.footer-links a:hover \{\s*color: #fff;\s*\}/g, '.footer-links a:hover {\n  color: #09090b;\n}');

  // Fix grid and glow for light background
  css = css.replace(/radial-gradient\(\s*rgba\(\s*255,\s*255,\s*255,\s*0\.1[^\)]*\)/g, 'radial-gradient(rgba(0, 0, 0, 0.04)');
  css = css.replace(/background-color: rgba\(\s*255,\s*255,\s*255,\s*0\.1\s*\);/g, 'background-color: rgba(0, 0, 0, 0.03);');
  css = css.replace(/background-color: rgba\(\s*255,\s*255,\s*255,\s*0\.05\s*\);/g, 'background-color: rgba(0, 0, 0, 0.02);');

  // Improve inputs
  css = css.replace(/border: 1px solid rgba\(228, 228, 231, 0\.6\);/g, 'border: 1px solid #d4d4d8;\n  box-shadow: 0 1px 2px rgba(0,0,0,0.02);');
  css = css.replace(/border-color: #6C3FF5;/g, 'border-color: #09090b;\n  box-shadow: 0 0 0 2px rgba(9, 9, 11, 0.1);');
  
  // Beautify button
  css = css.replace(/\.submit-btn \{\s*width: 100%;\s*height: 3rem;\s*background-color: #09090b;\s*color: #ffffff;\s*font-size: 1rem;\s*font-weight: 500;\s*border: none;\s*border-radius: 0\.5rem;\s*cursor: pointer;\s*transition: opacity 0\.2s;\s*\}/g, '.submit-btn {\n  width: 100%;\n  height: 3rem;\n  background-color: #09090b;\n  color: #ffffff;\n  font-size: 1rem;\n  font-weight: 500;\n  border: none;\n  border-radius: 0.5rem;\n  cursor: pointer;\n  transition: all 0.2s ease;\n  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1), 0 2px 4px -1px rgba(0,0,0,0.06);\n}');
  
  css = css.replace(/\.submit-btn:hover \{\s*opacity: 0\.9;\s*\}/g, '.submit-btn:hover {\n  transform: translateY(-1px);\n  box-shadow: 0 6px 10px -1px rgba(0,0,0,0.15), 0 2px 4px -1px rgba(0,0,0,0.05);\n  background-color: #18181b;\n}');

  // Fix 'Forgot password' color
  css = css.replace(/color: #6C3FF5;/g, 'color: #09090b;');

  // Make checkbox look nicer
  if (!css.includes('accent-color: #09090b')) {
    css = css.replace(/<input\s+type="checkbox"\s+v-model="form.remember"\s*\/>/g, '<input type="checkbox" v-model="form.remember" style="accent-color: #09090b; width: 16px; height: 16px; cursor: pointer;" />');
  }

  // Smooth characters
  if (!css.includes('filter: drop-shadow')) {
    css = css.replace(/\.characters \{/g, '.characters {\n  filter: drop-shadow(0 15px 25px rgba(0,0,0,0.1));');
  }

  fs.writeFileSync(file, css);
}

['web/src/views/LoginView.vue', 'web/src/views/RegisterView.vue'].forEach(beautify);
console.log('Styles beautified');