package com.example.util

import com.example.data.model.RnaApp

object StarterApps {

    const val ASTRO_DODGE_HTML = """<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
    <title>Astro Dodge</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; user-select: none; }
        body { background: #08090E; color: #FFF; font-family: system-ui, -apple-system, sans-serif; overflow: hidden; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; }
        #gameCanvas { background: radial-gradient(circle at 50% 50%, #151928 0%, #06070B 100%); border: 2px solid #00E5FF; border-radius: 16px; box-shadow: 0 0 30px rgba(0, 229, 255, 0.25); max-width: 96vw; max-height: 80vh; }
        .hud { position: absolute; top: 16px; left: 24px; right: 24px; display: flex; justify-content: space-between; font-weight: 700; font-size: 1.1rem; pointer-events: none; }
        .pill { background: rgba(18, 22, 34, 0.85); backdrop-filter: blur(8px); padding: 8px 16px; border-radius: 20px; border: 1px solid rgba(0, 229, 255, 0.4); color: #00E5FF; }
        .controls { margin-top: 12px; display: flex; gap: 12px; }
        button { background: #1B2030; color: #00E5FF; border: 1px solid #00E5FF; padding: 10px 20px; border-radius: 12px; font-weight: bold; cursor: pointer; }
        button:active { background: #00E5FF; color: #000; }
    </style>
</head>
<body>
    <div class="hud">
        <div class="pill">SCORE: <span id="scoreText">0</span></div>
        <div class="pill">SHIELDS: <span id="shieldText">❤❤❤</span></div>
    </div>
    <canvas id="gameCanvas" width="480" height="640"></canvas>
    <div class="controls">
        <button id="leftBtn">◀ LEFT</button>
        <button id="fireBtn">⚡ FIRE</button>
        <button id="rightBtn">RIGHT ▶</button>
    </div>

    <script>
        const canvas = document.getElementById('gameCanvas');
        const ctx = canvas.getContext('2d');
        const scoreText = document.getElementById('scoreText');
        const shieldText = document.getElementById('shieldText');

        let player = { x: 240, y: 560, w: 32, h: 36, speed: 7, shields: 3 };
        let bullets = [];
        let asteroids = [];
        let stars = [];
        let score = 0;
        let gameOver = false;
        let keys = {};

        // Generate parallax stars
        for (let i = 0; i < 60; i++) {
            stars.push({ x: Math.random() * canvas.width, y: Math.random() * canvas.height, s: Math.random() * 2 + 1, v: Math.random() * 1.5 + 0.5 });
        }

        window.addEventListener('keydown', e => {
            keys[e.code] = true;
            if (e.code === 'Space' || e.code === 'Enter') shoot();
            if (e.code === 'KeyR' && gameOver) resetGame();
        });
        window.addEventListener('keyup', e => { keys[e.code] = false; });

        document.getElementById('leftBtn').addEventListener('pointerdown', () => keys['ArrowLeft'] = true);
        document.getElementById('leftBtn').addEventListener('pointerup', () => keys['ArrowLeft'] = false);
        document.getElementById('rightBtn').addEventListener('pointerdown', () => keys['ArrowRight'] = true);
        document.getElementById('rightBtn').addEventListener('pointerup', () => keys['ArrowRight'] = false);
        document.getElementById('fireBtn').addEventListener('click', () => shoot());

        // Mouse/pointer aiming
        canvas.addEventListener('pointermove', e => {
            const rect = canvas.getBoundingClientRect();
            const scaleX = canvas.width / rect.width;
            player.x = (e.clientX - rect.left) * scaleX;
            if (player.x < 20) player.x = 20;
            if (player.x > canvas.width - 20) player.x = canvas.width - 20;
        });
        canvas.addEventListener('pointerdown', () => shoot());

        function shoot() {
            if (gameOver) { resetGame(); return; }
            bullets.push({ x: player.x, y: player.y - 18, vy: -12 });
        }

        function resetGame() {
            player.shields = 3;
            score = 0;
            bullets = [];
            asteroids = [];
            gameOver = false;
            shieldText.textContent = '❤❤❤';
            scoreText.textContent = '0';
        }

        function spawnAsteroid() {
            if (Math.random() < 0.04 && asteroids.length < 10) {
                asteroids.push({
                    x: Math.random() * (canvas.width - 40) + 20,
                    y: -30,
                    r: Math.random() * 14 + 12,
                    vx: (Math.random() - 0.5) * 2,
                    vy: Math.random() * 2.5 + 1.8
                });
            }
        }

        function update() {
            if (gameOver) return;

            // Player movement
            if (keys['ArrowLeft'] || keys['KeyA']) player.x = Math.max(20, player.x - player.speed);
            if (keys['ArrowRight'] || keys['KeyD']) player.x = Math.min(canvas.width - 20, player.x + player.speed);

            // Bullets
            for (let i = bullets.length - 1; i >= 0; i--) {
                bullets[i].y += bullets[i].vy;
                if (bullets[i].y < -10) bullets.splice(i, 1);
            }

            // Asteroids
            spawnAsteroid();
            for (let i = asteroids.length - 1; i >= 0; i--) {
                const a = asteroids[i];
                a.x += a.vx;
                a.y += a.vy;

                // Bullet hit detection
                for (let j = bullets.length - 1; j >= 0; j--) {
                    const b = bullets[j];
                    const dist = Math.hypot(a.x - b.x, a.y - b.y);
                    if (dist < a.r + 4) {
                        asteroids.splice(i, 1);
                        bullets.splice(j, 1);
                        score += 100;
                        scoreText.textContent = score;
                        break;
                    }
                }

                // Player collision
                if (asteroids[i]) {
                    const playerDist = Math.hypot(a.x - player.x, a.y - player.y);
                    if (playerDist < a.r + 16) {
                        asteroids.splice(i, 1);
                        player.shields--;
                        shieldText.textContent = '❤'.repeat(Math.max(0, player.shields));
                        if (player.shields <= 0) {
                            gameOver = true;
                        }
                    } else if (a.y > canvas.height + 30) {
                        asteroids.splice(i, 1);
                    }
                }
            }

            // Starfield
            stars.forEach(s => {
                s.y += s.v;
                if (s.y > canvas.height) s.y = 0;
            });
        }

        function draw() {
            ctx.clearRect(0, 0, canvas.width, canvas.height);

            // Stars
            ctx.fillStyle = '#6E7C9C';
            stars.forEach(s => {
                ctx.beginPath();
                ctx.arc(s.x, s.y, s.s, 0, Math.PI * 2);
                ctx.fill();
            });

            // Player Ship
            ctx.save();
            ctx.translate(player.x, player.y);
            // Engine glow
            ctx.fillStyle = '#00E5FF';
            ctx.shadowColor = '#00E5FF';
            ctx.shadowBlur = 15;
            ctx.beginPath();
            ctx.moveTo(-10, 15);
            ctx.lineTo(0, 25 + Math.random() * 8);
            ctx.lineTo(10, 15);
            ctx.fill();

            // Ship Body
            ctx.fillStyle = '#FFF';
            ctx.beginPath();
            ctx.moveTo(0, -18);
            ctx.lineTo(16, 14);
            ctx.lineTo(0, 8);
            ctx.lineTo(-16, 14);
            ctx.closePath();
            ctx.fill();

            // Cockpit
            ctx.fillStyle = '#7C4DFF';
            ctx.beginPath();
            ctx.arc(0, -2, 4, 0, Math.PI * 2);
            ctx.fill();
            ctx.restore();

            // Bullets
            ctx.fillStyle = '#00E5FF';
            ctx.shadowColor = '#00E5FF';
            ctx.shadowBlur = 10;
            bullets.forEach(b => {
                ctx.fillRect(b.x - 2, b.y, 4, 12);
            });
            ctx.shadowBlur = 0;

            // Asteroids
            ctx.fillStyle = '#8996B2';
            ctx.strokeStyle = '#B3C0DB';
            ctx.lineWidth = 2;
            asteroids.forEach(a => {
                ctx.beginPath();
                ctx.arc(a.x, a.y, a.r, 0, Math.PI * 2);
                ctx.fill();
                ctx.stroke();
            });

            // Game Over overlay
            if (gameOver) {
                ctx.fillStyle = 'rgba(6, 7, 11, 0.85)';
                ctx.fillRect(0, 0, canvas.width, canvas.height);
                ctx.fillStyle = '#FF5252';
                ctx.font = 'bold 36px sans-serif';
                ctx.textAlign = 'center';
                ctx.fillText('CRITICAL DAMAGE', canvas.width / 2, canvas.height / 2 - 20);
                ctx.fillStyle = '#FFF';
                ctx.font = '18px sans-serif';
                ctx.fillText('Final Score: ' + score, canvas.width / 2, canvas.height / 2 + 25);
                ctx.fillStyle = '#00E5FF';
                ctx.fillText('Press FIRE / ENTER to Restart', canvas.width / 2, canvas.height / 2 + 65);
            }

            requestAnimationFrame(draw);
        }

        function loop() {
            update();
            requestAnimationFrame(loop);
        }

        draw();
        loop();
    </script>
</body>
</html>"""

    const val NEOCALC_HTML = """<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
    <title>NeoCalc Express</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { background: #0E1017; color: #FFF; font-family: system-ui, -apple-system, sans-serif; display: flex; align-items: center; justify-content: center; min-height: 100vh; padding: 16px; }
        .calc-card { background: #151824; border: 1px solid #23283B; border-radius: 24px; padding: 24px; width: 100%; max-width: 380px; box-shadow: 0 16px 40px rgba(0,0,0,0.4); }
        .display-area { background: #0A0C12; border-radius: 16px; padding: 20px; text-align: right; margin-bottom: 20px; border: 1px solid #1E2333; }
        .prev-exp { font-size: 0.95rem; color: #7A86A1; min-height: 22px; word-break: break-all; }
        .main-val { font-size: 2.4rem; font-weight: 700; color: #00E5FF; word-break: break-all; margin-top: 4px; }
        .grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
        button { background: #1D2336; color: #F0F4FC; border: none; border-radius: 14px; font-size: 1.25rem; font-weight: 600; padding: 16px 0; cursor: pointer; transition: all 0.1s; }
        button:hover, button:focus { outline: 2px solid #00E5FF; background: #262E45; }
        button:active { transform: scale(0.96); }
        .op { color: #00E5FF; background: #172B3C; }
        .action { color: #FFB300; background: #2C2619; }
        .equals { background: linear-gradient(135deg, #00E5FF, #7C4DFF); color: #000; font-weight: bold; grid-column: span 2; }
    </style>
</head>
<body>
    <div class="calc-card">
        <div class="display-area">
            <div class="prev-exp" id="prevExp"></div>
            <div class="main-val" id="display">0</div>
        </div>
        <div class="grid">
            <button class="action" onclick="clearAll()">AC</button>
            <button class="action" onclick="deleteChar()">DEL</button>
            <button class="op" onclick="appendOp('%')">%</button>
            <button class="op" onclick="appendOp('/')">÷</button>

            <button onclick="appendNum('7')">7</button>
            <button onclick="appendNum('8')">8</button>
            <button onclick="appendNum('9')">9</button>
            <button class="op" onclick="appendOp('*')">×</button>

            <button onclick="appendNum('4')">4</button>
            <button onclick="appendNum('5')">5</button>
            <button onclick="appendNum('6')">6</button>
            <button class="op" onclick="appendOp('-')">−</button>

            <button onclick="appendNum('1')">1</button>
            <button onclick="appendNum('2')">2</button>
            <button onclick="appendNum('3')">3</button>
            <button class="op" onclick="appendOp('+')">+</button>

            <button onclick="appendNum('0')">0</button>
            <button onclick="appendNum('.')">.</button>
            <button class="equals" onclick="compute()">=</button>
        </div>
    </div>

    <script>
        let current = '0';
        let expression = '';

        const display = document.getElementById('display');
        const prevExp = document.getElementById('prevExp');

        function updateDisplay() {
            display.textContent = current;
            prevExp.textContent = expression;
        }

        function appendNum(num) {
            if (current === '0' && num !== '.') current = num;
            else if (num === '.' && current.includes('.')) return;
            else current += num;
            updateDisplay();
        }

        function appendOp(op) {
            expression = current + ' ' + op + ' ';
            current = '0';
            updateDisplay();
        }

        function clearAll() {
            current = '0';
            expression = '';
            updateDisplay();
        }

        function deleteChar() {
            if (current.length > 1) current = current.slice(0, -1);
            else current = '0';
            updateDisplay();
        }

        function compute() {
            if (!expression) return;
            try {
                const fullExp = expression + current;
                const sanitized = fullExp.replace(/×/g, '*').replace(/÷/g, '/').replace(/−/g, '-');
                const result = Function('"use strict";return (' + sanitized + ')')();
                expression = fullExp + ' =';
                current = String(Number(result.toFixed(6)));
                updateDisplay();
                expression = '';
            } catch(e) {
                current = 'Error';
                updateDisplay();
            }
        }

        // Support keyboard & remote keys
        window.addEventListener('keydown', e => {
            if (e.key >= '0' && e.key <= '9') appendNum(e.key);
            else if (e.key === '.') appendNum('.');
            else if (e.key === '+' || e.key === '-' || e.key === '*' || e.key === '/') appendOp(e.key);
            else if (e.key === 'Enter' || e.key === '=') compute();
            else if (e.key === 'Backspace') deleteChar();
            else if (e.key === 'Escape') clearAll();
        });
    </script>
</body>
</html>"""

    const val PLAYGROUND_HTML = """<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>RNA Web Playground</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { background: #0E1017; color: #FFF; font-family: monospace; display: flex; flex-direction: column; height: 100vh; }
        header { background: #161A26; padding: 12px 20px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #232A3E; }
        h1 { font-size: 1.1rem; color: #00E5FF; }
        .btn-run { background: #00E5FF; color: #000; border: none; padding: 8px 18px; border-radius: 8px; font-weight: bold; cursor: pointer; }
        .container { display: flex; flex: 1; overflow: hidden; }
        @media(max-width: 600px) { .container { flex-direction: column; } }
        .editor-pane { flex: 1; display: flex; flex-direction: column; border-right: 1px solid #232A3E; background: #0A0C13; }
        textarea { flex: 1; background: transparent; color: #E0E7FF; padding: 16px; border: none; outline: none; font-family: monospace; font-size: 14px; resize: none; line-height: 1.5; }
        .preview-pane { flex: 1; background: #FFF; }
        iframe { width: 100%; height: 100%; border: none; }
    </style>
</head>
<body>
    <header>
        <h1>⚡ RNA Web Playground</h1>
        <button class="btn-run" onclick="runCode()">▶ RUN CODE</button>
    </header>
    <div class="container">
        <div class="editor-pane">
            <textarea id="codeEditor" spellcheck="false">&lt;!DOCTYPE html&gt;
&lt;html&gt;
&lt;head&gt;
    &lt;style&gt;
        body { background: #12141F; color: #00E5FF; font-family: sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; margin: 0; text-align: center; }
        .card { background: #1A1E2E; padding: 32px; border-radius: 20px; border: 1px solid #2D3650; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
        h2 { margin-bottom: 12px; color: #FFF; }
        button { background: #7C4DFF; color: white; border: none; padding: 12px 24px; border-radius: 12px; font-weight: bold; font-size: 16px; cursor: pointer; margin-top: 16px; }
    &lt;/style&gt;
&lt;/head&gt;
&lt;body&gt;
    &lt;div class="card"&gt;
        &lt;h2&gt;Hello from RNA Sandbox!&lt;/h2&gt;
        &lt;p&gt;Interactive HTML, CSS &amp; JavaScript Runner&lt;/p&gt;
        &lt;button onclick="celebrate()"&gt;Click Me!&lt;/button&gt;
    &lt;/div&gt;
    &lt;script&gt;
        let count = 0;
        function celebrate() {
            count++;
            alert('RNA Container Clicked ' + count + ' times!');
        }
    &lt;/script&gt;
&lt;/body&gt;
&lt;/html&gt;</textarea>
        </div>
        <div class="preview-pane">
            <iframe id="preview"></iframe>
        </div>
    </div>
    <script>
        function runCode() {
            const code = document.getElementById('codeEditor').value;
            const iframe = document.getElementById('preview');
            iframe.srcdoc = code;
        }
        runCode();
    </script>
</body>
</html>"""

    val LEGACY_TEST_APP_IDS = listOf(
        "app_astro_dodge",
        "app_neocalc",
        "app_playground"
    )

    fun getInitialApps(): List<RnaApp> {
        return emptyList()
    }
}
