// Copies the jars the add-on compiles against into forge-1.20.1/libs and neoforge-1.21.1/libs: the Voltex release jars
// from the Voltex build folders, JEI and Jade from the mods of the add-on's test instances, and KubeJS with what it
// stands on from the local Gradle cache, where the Voltex builds left them (nothing is downloaded).
// The versions come from mod.properties.
// Run: node tools/refresh_libs.js [D:/IC3] [D:/IC3-neo]
const fs = require('fs'), path = require('path');

const ROOT = path.join(__dirname, '..');
const props = Object.fromEntries(fs.readFileSync(path.join(ROOT, 'mod.properties'), 'utf8').split(/\r?\n/)
  .filter(l => l && !l.startsWith('#') && l.includes('=')).map(l => [l.slice(0, l.indexOf('=')), l.slice(l.indexOf('=') + 1)]));
const v = props.voltex_version;
const [forgeRepo = 'D:/IC3', neoRepo = 'D:/IC3-neo'] = process.argv.slice(2);
const mods = version => path.join(process.env.APPDATA, 'PrismLauncher/instances', 'Voltex Nuclear ' + version, 'minecraft/mods');
// a jar in the Gradle cache: caches/modules-2/files-2.1/<group>/<name>/<version>/<hash>/<name>-<version>.jar
const cached = (group, name, version) => {
  const dir = path.join(process.env.USERPROFILE, '.gradle/caches/modules-2/files-2.1', group, name, version);
  const file = name + '-' + version + '.jar';
  for (const hash of fs.existsSync(dir) ? fs.readdirSync(dir) : []) if (fs.existsSync(path.join(dir, hash, file))) return path.join(dir, hash, file);
  return path.join(dir, file);   // missing: reported below
};

for (const [from, dir] of [
  [path.join(forgeRepo, 'build/libs', `Voltex-${v}.jar`), 'forge-1.20.1'],
  [path.join(neoRepo, 'build/libs', `Voltex-${v}-neoforge-1.21.1.jar`), 'neoforge-1.21.1'],
  [path.join(mods('1.20.1'), `jei-1.20.1-forge-${props.jei_version_forge}.jar`), 'forge-1.20.1'],
  [path.join(mods('1.21.1'), `jei-1.21.1-neoforge-${props.jei_version_neoforge}.jar`), 'neoforge-1.21.1'],
  [path.join(mods('1.20.1'), 'Jade-1.20.1-Forge-11.13.2.jar'), 'forge-1.20.1'],
  [path.join(mods('1.21.1'), 'jade-15.10.6+neoforge.jar'), 'neoforge-1.21.1'],
  [cached('dev.latvian.mods', 'kubejs-forge', props.kubejs_version_forge), 'forge-1.20.1'],
  [cached('dev.latvian.mods', 'rhino-forge', props.rhino_version_forge), 'forge-1.20.1'],
  [cached('dev.architectury', 'architectury-forge', props.architectury_version_forge), 'forge-1.20.1'],
  [cached('dev.latvian.mods', 'kubejs-neoforge', props.kubejs_version_neoforge), 'neoforge-1.21.1'],
  [cached('dev.latvian.mods', 'rhino', props.rhino_version_neoforge), 'neoforge-1.21.1'],
]) {
  if (!fs.existsSync(from)) throw new Error('missing ' + from + ' (build Voltex ' + v + ' first)');
  const to = path.join(ROOT, dir, 'libs', path.basename(from));
  fs.mkdirSync(path.dirname(to), { recursive: true });
  fs.copyFileSync(from, to);
  console.log(dir + '/libs/' + path.basename(from), fs.statSync(to).size, 'bytes');
}
