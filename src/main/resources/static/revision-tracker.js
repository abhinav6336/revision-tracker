const API_ROOT = ''; // uses same origin; if running on different port, set to http://localhost:8080
let todayFilterActive = false;

function getCurrentUser() {
    const user = localStorage.getItem('currentUser');
    return user ? JSON.parse(user) : null;
}

function getAuthHeaders() {
    const user = getCurrentUser();
    const headers = { "Content-Type": "application/json" };
    if (user && user.id) {
        headers["X-User-Id"] = user.id;
    }
    return headers;
}

function createTopicElement(item) {
    const topicBox = document.createElement("div");
    topicBox.classList.add("topic-box");
    topicBox.dataset.topic = item.topic || item['topic'];
    const nameDiv = document.createElement("div");
    nameDiv.classList.add("topic-name");
    nameDiv.textContent = item.topic || item['topic'];
    const revisionDiv = document.createElement("div");
    revisionDiv.classList.add("revision-dates");
    // autodates may be named 'autodates' or 'revisiondates' depending on server
    const dates = item.autodates || item.revisiondates || '';
    revisionDiv.textContent = (dates || (item.date || '')).trim().replace(/ /g, '\n');
    topicBox.dataset.autodates = dates || '';
    topicBox.dataset.date = item.date || '';
    topicBox.appendChild(nameDiv);
    topicBox.appendChild(revisionDiv);
    topicBox.addEventListener('click', () => {
        document.querySelectorAll('.topic-box').forEach(b => b.classList.remove('selected'));
        topicBox.classList.add('selected');
    });
    return topicBox;
}

let allTopicsCache = [];

function loadTopics() {
    const user = getCurrentUser();
    if (!user) {
        // Guest Mode: Load from LocalStorage
        console.log('Guest Mode: Loading local topics');
        const localTopics = JSON.parse(localStorage.getItem('guestTopics') || '[]');
        allTopicsCache = localTopics.map(t => ({
            topic: t.topic,
            revisiondates: t.dates ? t.dates.join('\n') : (t.date || ''),
            autodates: t.dates ? t.dates.join('\n') : (t.date || ''),
            date: t.date || ''
        }));

        renderTopics(allTopicsCache);
        return;
    }

    fetch(API_ROOT + '/topics', {
        headers: getAuthHeaders()
    })
        .then(r => r.json())
        .then(list => {
            allTopicsCache = list;
            renderTopics(list);
        })
        .catch(e => console.warn('Could not load topics', e));
}

function renderTopics(list) {
    const container = document.getElementById('today');
    container.innerHTML = '';
    if (list.length === 0) {
        const empty = document.createElement('div');
        empty.style.padding = '20px';
        empty.style.color = '#555';
        empty.textContent = 'No topics found. Add one above!';
        container.appendChild(empty);
        return;
    }
    list.forEach(item => {
        container.appendChild(createTopicElement(item));
    });
}

function addtopics() {
    const user = getCurrentUser();
    const topicInput = document.getElementById("topic-input");
    const topicname = topicInput.value.trim();
    if (!topicname) { alert("Please enter a topic name."); return; }

    if (!user) {
        // Guest Mode: Add to LocalStorage
        const localTopics = JSON.parse(localStorage.getItem('guestTopics') || '[]');
        // Simple duplication check
        if (localTopics.some(t => t.topic.toLowerCase() === topicname.toLowerCase())) {
            alert('Topic already exists!');
            return;
        }

        // Calculate revision dates (Simple imitation of backend logic or just current date)
        const today = new Date().toISOString().slice(0, 10);
        // For guest, let's just set today. Complex spaced repetition logic is typically backend, 
        // but we can simulate a simple one: Today, +3 days, +7 days, +30 days
        const addDays = (date, days) => {
            const result = new Date(date);
            result.setDate(result.getDate() + days);
            return result.toISOString().slice(0, 10);
        };
        const dates = [1, 3, 7, 14, 30, 90, 180, 365].map(n => addDays(today, n));

        const newTopic = {
            topic: topicname,
            date: dates.join('\n'),
            dates: dates
        };

        localTopics.push(newTopic);
        localStorage.setItem('guestTopics', JSON.stringify(localTopics));
        topicInput.value = '';
        loadTopics();
        return;
    }

    fetch(API_ROOT + '/topics', {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify({ topic: topicname })
    })
        .then(response => {
            if (response.ok) {
                topicInput.value = '';
                loadTopics();
            } else {
                response.text().then(t => alert('Failed to add topic: ' + t));
            }
        })
        .catch(error => { console.error("Error:", error); alert('Error adding topic'); });
}

// Open modal to choose topics to delete
document.getElementById('removeBtn').addEventListener('click', () => {
    openDeleteModal();
});

function openDeleteModal() {
    populateDeleteModal();
    document.getElementById('deleteModal').style.display = 'flex';
}
function closeDeleteModal() {
    document.getElementById('deleteModal').style.display = 'none';
}

function populateDeleteModal() {
    const list = document.getElementById('deleteList');
    list.innerHTML = '';
    // collect current topics from DOM
    const topics = Array.from(document.querySelectorAll('.topic-box')).map(b => ({
        topic: b.dataset.topic,
        title: b.querySelector('.topic-name')?.textContent || b.dataset.topic,
        date: b.dataset.date || ''
    }));
    if (topics.length === 0) { list.innerHTML = '<div style="padding:12px;color:#777">No topics available</div>'; return; }
    topics.forEach(t => {
        const row = document.createElement('div');
        row.style.display = 'flex'; row.style.alignItems = 'center'; row.style.gap = '8px'; row.style.padding = '6px';
        const cb = document.createElement('input'); cb.type = 'checkbox'; cb.value = t.topic;
        const label = document.createElement('div');
        label.textContent = t.title + (t.date ? ' (' + t.date + ')' : '');
        label.style.flex = '1';
        row.appendChild(cb); row.appendChild(label);
        list.appendChild(row);
    });
}

document.getElementById('cancelDelete').addEventListener('click', () => closeDeleteModal());
document.getElementById('confirmDelete').addEventListener('click', () => {
    const checked = Array.from(document.querySelectorAll('#deleteList input[type=checkbox]:checked')).map(i => i.value);
    if (checked.length === 0) { alert('Select one or more topics to delete'); return; }
    if (!confirm('Delete ' + checked.length + ' topic(s)?')) return;

    const user = getCurrentUser();
    if (!user) {
        // Guest Delete
        let localTopics = JSON.parse(localStorage.getItem('guestTopics') || '[]');
        localTopics = localTopics.filter(t => !checked.includes(t.topic));
        localStorage.setItem('guestTopics', JSON.stringify(localTopics));
        loadTopics();
        closeDeleteModal();
        return;
    }

    // delete sequentially and remove from DOM
    Promise.all(checked.map(topic =>
        fetch(API_ROOT + '/topics/delete', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({ topic })
        })
            .then(r => ({ topic, r }))
            .catch(e => ({ topic, error: e }))
    ))
        .then(results => {
            console.log('delete results', results);
            const failed = results.filter(r => !(r.r && r.r.ok));
            if (failed.length > 0) {
                console.warn('Some deletes failed', failed);
                alert('Some deletes failed — see console for details');
            }
            // Reload topics to reflect current server state (safer than trying to remove individual DOM nodes)
            loadTopics();
        })
        .finally(() => { closeDeleteModal(); });
});

document.getElementById('todayBtn').addEventListener('click', () => {
    const today = new Date().toISOString().slice(0, 10);
    const boxes = document.querySelectorAll('.topic-box');
    if (!todayFilterActive) {
        boxes.forEach(b => {
            const ad = b.dataset.autodates || '';
            if (ad.includes(today)) {
                b.style.display = 'flex';
            } else {
                b.style.display = 'none';
            }
        });
        todayFilterActive = true;
        document.getElementById('todayBtn').textContent = 'Show all';
    } else {
        boxes.forEach(b => b.style.display = 'flex');
        todayFilterActive = false;
        document.getElementById('todayBtn').textContent = 'Today';
    }
});

document.getElementById('quizBtn').addEventListener('click', () => {
    openQuizModal();
});

let currentQuizQuestions = [];
let currentQuizAnswers = {}; // { index: "Option Text" }

function openQuizModal() {
    const modal = document.getElementById('quizModal');
    modal.style.display = 'flex';
    renderQuizSetup();
}

function renderQuizSetup() {
    const container = document.getElementById('quizContent');
    container.innerHTML = '';

    const wrapper = document.createElement('div');
    wrapper.style.padding = '12px';

    const title = document.createElement('h3');
    title.textContent = 'Configure Quiz';
    title.style.marginTop = '0';
    wrapper.appendChild(title);

    // Topic Selection
    const topicLabel = document.createElement('p');
    topicLabel.textContent = 'Select Topics:';
    topicLabel.style.fontWeight = 'bold';
    wrapper.appendChild(topicLabel);

    const topicsDiv = document.createElement('div');
    topicsDiv.style.maxHeight = '200px';
    topicsDiv.style.overflowY = 'auto';
    topicsDiv.style.border = '1px solid #ddd';
    topicsDiv.style.padding = '8px';
    topicsDiv.style.marginBottom = '12px';
    topicsDiv.style.borderRadius = '4px';

    if (allTopicsCache.length === 0) {
        topicsDiv.textContent = 'No topics available. Add some topics first!';
    } else {
        allTopicsCache.forEach((t, i) => {
            const row = document.createElement('div');
            row.style.display = 'flex';
            row.style.gap = '8px';
            row.style.marginBottom = '4px';

            const cb = document.createElement('input');
            cb.type = 'checkbox';
            cb.name = 'quizTopic';
            cb.value = t.topic;
            cb.id = 'qt_' + i;
            cb.checked = true; // Default select all

            const lab = document.createElement('label');
            lab.htmlFor = 'qt_' + i;
            lab.textContent = t.topic;

            row.appendChild(cb);
            row.appendChild(lab);
            topicsDiv.appendChild(row);
        });
    }
    wrapper.appendChild(topicsDiv);

    // Helpers
    const btnRow = document.createElement('div');
    btnRow.style.display = 'flex';
    btnRow.style.gap = '8px';
    btnRow.style.marginBottom = '16px';

    const selectAllBtn = document.createElement('button');
    selectAllBtn.className = 'small';
    selectAllBtn.textContent = 'Select All';
    selectAllBtn.onclick = () => document.querySelectorAll('input[name=quizTopic]').forEach(c => c.checked = true);

    const selectNoneBtn = document.createElement('button');
    selectNoneBtn.className = 'small';
    selectNoneBtn.textContent = 'Clear All';
    selectNoneBtn.onclick = () => document.querySelectorAll('input[name=quizTopic]').forEach(c => c.checked = false);

    btnRow.appendChild(selectAllBtn);
    btnRow.appendChild(selectNoneBtn);
    wrapper.appendChild(btnRow);

    // Question Count
    const countLabel = document.createElement('p');
    countLabel.textContent = 'Number of Questions:';
    countLabel.style.fontWeight = 'bold';
    wrapper.appendChild(countLabel);

    const countInput = document.createElement('input');
    countInput.type = 'number';
    countInput.value = '5';
    countInput.min = '1';
    countInput.max = '20';
    countInput.style.padding = '8px';
    countInput.style.marginBottom = '20px';
    countInput.style.borderRadius = '4px';
    countInput.style.border = '1px solid #ccc';
    wrapper.appendChild(countInput);

    // Start Button
    const startBtn = document.createElement('button');
    startBtn.textContent = 'Start Quiz';
    startBtn.style.display = 'block';
    startBtn.style.width = '100%';
    startBtn.style.backgroundColor = 'var(--accent, #8b5e3c)';
    startBtn.style.color = '#fff';
    startBtn.onclick = () => {
        const selected = Array.from(document.querySelectorAll('input[name=quizTopic]:checked')).map(c => c.value);
        const count = parseInt(countInput.value);
        if (selected.length === 0) { alert('Select at least one topic'); return; }
        if (!count || count < 1) { alert('Enter a valid number of questions'); return; }
        startCustomQuiz(selected, count);
    };

    wrapper.appendChild(startBtn);
    container.appendChild(wrapper);
}

function startCustomQuiz(topics, count) {
    const container = document.getElementById('quizContent');
    container.innerHTML = '<div style="text-align:center;padding:20px">Generating quiz...<br><small>This may take a few seconds</small></div>';

    fetch(API_ROOT + '/api/quiz/generate', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ topics, count })
    })
        .then(r => r.json())
        .then(data => {
            if (data.message && !data.questions) {
                container.innerHTML = `<div style="text-align:center;padding:20px">${data.message}</div>`;
                return;
            }
            if (!data.questions || data.questions.length === 0) {
                container.innerHTML = `<div style="text-align:center;padding:20px">No questions returned.</div>`;
                return;
            }
            currentQuizQuestions = data.questions;
            currentQuizAnswers = {};
            renderQuizQuestions();
        })
        .catch(e => {
            container.innerHTML = `<div style="text-align:center;color:red;padding:20px">Error: ${e.message}</div>`;
        });
}

function renderQuizQuestions() {
    const container = document.getElementById('quizContent');
    container.innerHTML = '';

    const list = document.createElement('div');
    list.style.padding = '10px';

    currentQuizQuestions.forEach((q, index) => {
        const card = document.createElement('div');
        card.style.background = '#f9f9f9';
        card.style.padding = '16px';
        card.style.marginBottom = '16px';
        card.style.borderRadius = '8px';
        card.style.border = '1px solid #ddd';

        const title = document.createElement('h4');
        title.textContent = `Q${index + 1} (${q.topic}): ${q.question}`;
        title.style.margin = '0 0 12px 0';
        card.appendChild(title);

        const optionsDiv = document.createElement('div');
        optionsDiv.style.display = 'flex';
        optionsDiv.style.flexDirection = 'column';
        optionsDiv.style.gap = '8px';

        q.options.forEach(opt => {
            const btn = document.createElement('button');
            btn.textContent = opt;
            btn.style.textAlign = 'left';
            btn.style.background = '#fff';
            btn.style.border = '1px solid #ccc';
            btn.style.color = '#333';

            // Check if previously selected
            if (currentQuizAnswers[index] === opt) {
                btn.style.background = '#e3f2fd';
                btn.style.borderColor = '#2196f3';
                btn.style.fontWeight = 'bold';
            }

            btn.onclick = () => {
                currentQuizAnswers[index] = opt;
                // Re-render only this card's buttons or just update styles visually
                Array.from(optionsDiv.children).forEach(b => {
                    b.style.background = '#fff';
                    b.style.borderColor = '#ccc';
                    b.style.fontWeight = 'normal';
                });
                btn.style.background = '#e3f2fd';
                btn.style.borderColor = '#2196f3';
                btn.style.fontWeight = 'bold';
            };
            optionsDiv.appendChild(btn);
        });
        card.appendChild(optionsDiv);
        list.appendChild(card);
    });

    const submitBtn = document.createElement('button');
    submitBtn.textContent = 'Submit Quiz';
    submitBtn.style.width = '100%';
    submitBtn.style.padding = '14px';
    submitBtn.style.backgroundColor = '#2e7d32'; // Green
    submitBtn.style.color = '#fff';
    submitBtn.style.marginBottom = '20px';
    submitBtn.onclick = submitQuiz;

    list.appendChild(submitBtn);
    container.appendChild(list);
}

function submitQuiz() {
    // Calculate score
    let score = 0;
    currentQuizQuestions.forEach((q, i) => {
        if (currentQuizAnswers[i] === q.answer) {
            score++;
        }
    });

    renderQuizResults(score);
}

function renderQuizResults(score) {
    const container = document.getElementById('quizContent');
    container.innerHTML = '';

    const total = currentQuizQuestions.length;
    const percentage = Math.round((score / total) * 100);

    const header = document.createElement('div');
    header.style.textAlign = 'center';
    header.style.padding = '20px';
    header.style.background = '#e8f5e9';
    header.style.marginTop = '16px';
    header.style.marginBottom = '16px';
    header.style.borderRadius = '8px';
    header.innerHTML = `<h2 style="margin:0">Score: ${score} / ${total}</h2><p style="margin:5px 0 0 0;font-size:18px">${percentage}%</p>`;

    // Re-render questions with feedback
    const list = document.createElement('div');
    currentQuizQuestions.forEach((q, index) => {
        const userAns = currentQuizAnswers[index];
        const isCorrect = userAns === q.answer;

        const card = document.createElement('div');
        card.style.background = '#fff';
        card.style.padding = '14px';
        card.style.marginBottom = '12px';
        card.style.borderRadius = '8px';
        card.style.border = isCorrect ? '2px solid #c3e6cb' : '2px solid #f5c6cb';

        const title = document.createElement('h4');
        title.textContent = `Q${index + 1}: ${q.question}`;
        title.style.margin = '0 0 8px 0';
        card.appendChild(title);

        const feedback = document.createElement('div');
        if (isCorrect) {
            feedback.innerHTML = `<span style="color:green;font-weight:bold">✅ Correct!</span> You chose: ${userAns}`;
        } else {
            feedback.innerHTML = `<span style="color:red;font-weight:bold">❌ Incorrect.</span><br>You chose: ${userAns || 'Nothing'}<br>Correct Answer: <span style="font-weight:bold">${q.answer}</span>`;
        }
        card.appendChild(feedback);
        list.appendChild(card);
    });


    const closeBtn = document.createElement('button');
    closeBtn.textContent = 'Close / New Quiz';
    closeBtn.style.marginTop = '10px';
    closeBtn.style.width = '100%';
    closeBtn.onclick = () => renderQuizSetup(); // Go back to setup

    container.appendChild(list);
    container.appendChild(header);
    container.appendChild(closeBtn);
}
document.getElementById('competeBtn').addEventListener('click', () => alert('Compete feature coming soon'));
document.getElementById('timetableBtn').addEventListener('click', () => {
    const p = document.getElementById('placeholder');
    p.style.display = p.style.display === 'block' ? 'none' : 'block';
    p.textContent = 'Timetable view placeholder — integrate your timetable here.';
});

document.getElementById('todoBtn').addEventListener('click', () => {
    const todo = prompt('Quick add to-do item:');
    if (!todo) return;
    const arr = JSON.parse(localStorage.getItem('rtodos') || '[]'); arr.push({ text: todo, at: new Date().toISOString() }); localStorage.setItem('rtodos', JSON.stringify(arr));
    alert('To-do added (stored locally).');
});

// Login Modal
document.getElementById('loginBtn').addEventListener('click', () => {
    document.getElementById('loginModal').style.display = 'flex';
    document.getElementById('loginError').textContent = '';
});
document.getElementById('loginCancel').addEventListener('click', () => {
    document.getElementById('loginModal').style.display = 'none';
});
document.getElementById('loginSubmit').addEventListener('click', () => {
    const username = document.getElementById('loginUsername').value.trim();
    const password = document.getElementById('loginPassword').value.trim();
    const errorDiv = document.getElementById('loginError');

    if (!username || !password) {
        errorDiv.textContent = 'Username and password are required';
        return;
    }

    fetch(API_ROOT + '/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    })
        .then(r => r.json())
        .then(data => {
            if (data.success) {
                localStorage.setItem('currentUser', JSON.stringify(data.user));
                alert('Login successful!');
                document.getElementById('loginModal').style.display = 'none';
                document.getElementById('loginUsername').value = '';
                document.getElementById('loginPassword').value = '';
                renderAuthUI();
                loadTopics();
            } else {
                errorDiv.textContent = data.message || 'Login failed';
            }
        })
        .catch(e => {
            errorDiv.textContent = 'Error: ' + e.message;
        });
});

// Signup Modal
document.getElementById('signupBtn').addEventListener('click', () => {
    document.getElementById('signupModal').style.display = 'flex';
    document.getElementById('signupError').textContent = '';
});
document.getElementById('signupCancel').addEventListener('click', () => {
    document.getElementById('signupModal').style.display = 'none';
});
document.getElementById('signupSubmit').addEventListener('click', () => {
    const username = document.getElementById('signupUsername').value.trim();
    const email = document.getElementById('signupEmail').value.trim();
    const password = document.getElementById('signupPassword').value.trim();
    const errorDiv = document.getElementById('signupError');

    if (!username || !email || !password) {
        errorDiv.textContent = 'All fields are required';
        return;
    }

    if (password.length < 6) {
        errorDiv.textContent = 'Password must be at least 6 characters';
        return;
    }

    fetch(API_ROOT + '/api/auth/signup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, email, password })
    })
        .then(r => r.json())
        .then(data => {
            if (data.success) {
                alert(data.message);
                document.getElementById('signupModal').style.display = 'none';
                document.getElementById('signupUsername').value = '';
                document.getElementById('signupEmail').value = '';
                document.getElementById('signupPassword').value = '';
            } else {
                errorDiv.textContent = data.message || 'Signup failed';
            }
        })
        .catch(e => {
            errorDiv.textContent = 'Error: ' + e.message;
        });
});

// Forgot Password Modal
document.getElementById('forgotCancel').addEventListener('click', () => {
    document.getElementById('forgotModal').style.display = 'none';
});
document.getElementById('forgotSubmit').addEventListener('click', () => {
    const email = document.getElementById('forgotEmail').value.trim();
    const errorDiv = document.getElementById('forgotError');

    if (!email) {
        errorDiv.textContent = 'Email is required';
        return;
    }

    fetch(API_ROOT + '/api/auth/forgot-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email })
    })
        .then(r => r.json())
        .then(data => {
            if (data.success) {
                alert(data.message);
                document.getElementById('forgotModal').style.display = 'none';
                document.getElementById('forgotEmail').value = '';
            } else {
                errorDiv.textContent = data.message || 'Failed to send reset link';
            }
        })
        .catch(e => {
            errorDiv.textContent = 'Error: ' + e.message;
        });
});

// Reset Password Modal
document.getElementById('resetCancel').addEventListener('click', () => {
    document.getElementById('resetModal').style.display = 'none';
    // Clean URL
    window.history.replaceState({}, document.title, window.location.pathname);
});
document.getElementById('resetSubmit').addEventListener('click', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');
    const password = document.getElementById('resetPassword').value.trim();
    const confirm = document.getElementById('resetConfirm').value.trim();
    const errorDiv = document.getElementById('resetError');

    if (!token) {
        errorDiv.textContent = 'Missing reset token';
        return;
    }

    if (!password || !confirm) {
        errorDiv.textContent = 'Both fields are required';
        return;
    }

    if (password !== confirm) {
        errorDiv.textContent = 'Passwords do not match';
        return;
    }

    if (password.length < 6) {
        errorDiv.textContent = 'Password must be at least 6 characters';
        return;
    }

    fetch(API_ROOT + '/api/auth/reset-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token, password })
    })
        .then(r => r.json())
        .then(data => {
            if (data.success) {
                alert(data.message);
                document.getElementById('resetModal').style.display = 'none';
                document.getElementById('loginModal').style.display = 'flex';
                document.getElementById('resetPassword').value = '';
                document.getElementById('resetConfirm').value = '';
                // Clean URL
                window.history.replaceState({}, document.title, window.location.pathname);
            } else {
                errorDiv.textContent = data.message || 'Reset failed';
            }
        })
        .catch(e => {
            errorDiv.textContent = 'Error: ' + e.message;
        });
});

// Auth UI helpers
function renderAuthUI() {
    const user = getCurrentUser();
    const userDisplay = document.getElementById('userDisplay');
    const logoutBtn = document.getElementById('logoutBtn');
    const loginBtn = document.getElementById('loginBtn');
    const signupBtn = document.getElementById('signupBtn');
    if (user && user.username) {
        userDisplay.textContent = user.username;
        userDisplay.style.display = 'block';
        logoutBtn.style.display = 'inline-block';
        loginBtn.style.display = 'none';
        signupBtn.style.display = 'none';
    } else {
        userDisplay.style.display = 'none';
        logoutBtn.style.display = 'none';
        loginBtn.style.display = 'inline-block';
        signupBtn.style.display = 'inline-block';
    }
}

function logoutUser() {
    localStorage.removeItem('currentUser');
    renderAuthUI();
    document.getElementById('today').innerHTML = '';
}

document.getElementById('logoutBtn').addEventListener('click', () => {
    if (confirm('Logout?')) logoutUser();
});

// Load topics and render auth UI on start
document.addEventListener('DOMContentLoaded', () => {
    renderAuthUI();
    loadTopics();

    // Check for reset token
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('token')) {
        document.getElementById('resetModal').style.display = 'flex';
    }
});
