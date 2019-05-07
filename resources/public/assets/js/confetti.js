var maxParticleCount = 150; //set max confetti count
var particleSpeed = 2; //set the particle animation speed
var confettiFrameInterval = 20; //the confetti animation frame interval
var startConfetti; //call to start confetti animation
var stopConfetti; //call to stop adding confetti
var toggleConfetti; //call to start or stop the confetti animation depending on whether it's already running
var pauseConfetti; //call to freeze confetti animation
var resumeConfetti; //call to unfreeze confetti animation
var toggleConfettiPause; //call to toggle whether the confetti animation is paused
var removeConfetti; //call to stop the confetti animation and remove all confetti immediately
var isConfettiPaused; //call and returns true or false depending on whether the confetti animation is paused
var isConfettiRunning; //call and returns true or false depending on whether the animation is running

(function() {
	startConfetti = startConfettiInner;
	stopConfetti = stopConfettiInner;
	toggleConfetti = toggleConfettiInner;
	pauseConfetti = pauseConfettiInner;
	resumeConfetti = resumeConfettiInner;
	toggleConfettiPause = toggleConfettiPauseInner;
	isConfettiPaused = isConfettiPausedInner;
	removeConfetti = removeConfettiInner;
	isConfettiRunning = isConfettiRunningInner;
	var supportsAnimationFrame = window.requestAnimationFrame || window.webkitRequestAnimationFrame || window.mozRequestAnimationFrame || window.oRequestAnimationFrame || window.msRequestAnimationFrame;
	var colors = ["#4ec2c4", "#32abde"]
	var streamingConfetti = false;
	var animationTimer = null;
	var pause = false;
	var lastFrameTime = Date.now();
	var particles = [];
	var waveAngle = 0;
	var context = null;
	
	function resetParticle(particle, width, height) {
		particle.color = colors[(Math.random() * colors.length) | 0];
		particle.x = Math.random() * width;
		particle.y = Math.random() * height - height;
		particle.diameter = Math.random() * 10 + 5;
		particle.tilt = Math.random() * 10 - 10;
		particle.tiltAngleIncrement = Math.random() * 0.07 + 0.05;
		particle.tiltAngle = 0;
		return particle;
	}

	function toggleConfettiPauseInner() {
		if (pause)
			resumeConfettiInner();
		else
			pauseConfettiInner();
	}

	function isConfettiPausedInner() {
		return pause;
	}

	function pauseConfettiInner() {
		pause = true;
	}

	function resumeConfettiInner() {
		pause = false;
		runAnimation();
	}

	function runAnimation() {
		if (pause)
			return;
		if (particles.length === 0) {
			context.clearRect(0, 0, window.innerWidth, window.innerHeight);
			animationTimer = null;
		} else {
			var now = Date.now();
			var delta = now - lastFrameTime;
			if (!supportsAnimationFrame || delta > confettiFrameInterval) {
				context.clearRect(0, 0, window.innerWidth, window.innerHeight);
				updateParticles();
				drawParticles(context);
				lastFrameTime = now - (delta % confettiFrameInterval);
			}
			animationTimer = requestAnimationFrame(runAnimation);
		}
	}

	function startConfettiInner() {
		var width = window.innerWidth;
		var height = window.innerHeight;
		window.requestAnimationFrame = (function() {
			return window.requestAnimationFrame ||
				window.webkitRequestAnimationFrame ||
				window.mozRequestAnimationFrame ||
				window.oRequestAnimationFrame ||
				window.msRequestAnimationFrame ||
				function (callback) {
					return window.setTimeout(callback, confettiFrameInterval);
				};
		})();
		var canvas = document.getElementById("confetti-canvas");
		if (canvas === null) {
			canvas = document.createElement("canvas");
			canvas.setAttribute("id", "confetti-canvas");
			canvas.setAttribute("style", "display:block;z-index:999999;pointer-events:none");
			document.body.appendChild(canvas);
			canvas.width = width;
			canvas.height = height;
			window.addEventListener("resize", function() {
				canvas.width = window.innerWidth;
				canvas.height = window.innerHeight;
			}, true);
			context = canvas.getContext("2d");
		}
		while (particles.length < maxParticleCount)
			particles.push(resetParticle({}, width, height));
		streamingConfetti = true;
		pause = false;
		runAnimation();
	}

	function stopConfettiInner() {
		streamingConfetti = false;
	}

	function removeConfettiInner() {
		stopConfetti();
		pause = false;
		particles = [];
	}

	function toggleConfettiInner() {
		if (streamingConfetti)
			stopConfettiInner();
		else
			startConfettiInner();
	}
	
	function isConfettiRunningInner() {
		return streamingConfetti;
	}

	function drawParticles(context) {
		var particle;
		var x;
		for (var i = 0; i < particles.length; i++) {
			particle = particles[i];
			context.beginPath();
			context.lineWidth = particle.diameter;
			context.strokeStyle = particle.color;
			x = particle.x + particle.tilt;
			context.moveTo(x + particle.diameter / 2, particle.y);
			context.lineTo(x, particle.y + particle.tilt + particle.diameter / 2);
			context.stroke();
		}
	}

	function updateParticles() {
		var width = window.innerWidth;
		var height = window.innerHeight;
		var particle;
		waveAngle += 0.01;
		for (var i = 0; i < particles.length; i++) {
			particle = particles[i];
			if (!streamingConfetti && particle.y < -15)
				particle.y = height + 100;
			else {
				particle.tiltAngle += particle.tiltAngleIncrement;
				particle.x += Math.sin(waveAngle);
				particle.y += (Math.cos(waveAngle) + particle.diameter + particleSpeed) * 0.5;
				particle.tilt = Math.sin(particle.tiltAngle) * 15;
			}
			if (particle.x > width + 20 || particle.x < -20 || particle.y > height) {
				if (streamingConfetti && particles.length <= maxParticleCount)
					resetParticle(particle, width, height);
				else {
					particles.splice(i, 1);
					i--;
				}
			}
		}
	}
})();
