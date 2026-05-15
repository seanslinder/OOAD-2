let selectedRideId = null;

async function fetchRides() {
    try {
        const response = await fetch('/api/rides');
        const rides = await response.json();
        renderRides(rides);
    } catch (error) {
        console.error('Error fetching rides:', error);
    }
}

function renderRides(rides) {
    const list = document.getElementById('ride-list');
    const activeContainer = document.getElementById('active-rides');
    list.innerHTML = '';
    activeContainer.innerHTML = '';

    let hasActive = false;

    rides.forEach(ride => {
        const tr = document.createElement('tr');
        const isActive = ride.status === 'ACTIVE';
        
        tr.innerHTML = `
            <td>${ride.id.substring(0, 8)}...</td>
            <td>${ride.userId}</td>
            <td>${formatDate(ride.startTime)}</td>
            <td>${ride.endTime ? formatDate(ride.endTime) : '-'}</td>
            <td>${ride.distanceKm ? ride.distanceKm.toFixed(2) : '0.00'}</td>
            <td>${ride.cost ? '$' + ride.cost.toFixed(2) : '-'}</td>
            <td class="${isActive ? 'status-active' : 'status-completed'}">${ride.status}</td>
            <td>
                ${isActive ? `<button class="small" onclick="openEndRideModal('${ride.id}')">End Ride</button>` : ''}
            </td>
        `;
        list.appendChild(tr);

        if (isActive) {
            hasActive = true;
            const activeDiv = document.createElement('div');
            activeDiv.className = 'active-ride-item';
            activeDiv.innerHTML = `
                <p><strong>Ride:</strong> ${ride.id.substring(0, 8)}... <strong>User:</strong> ${ride.userId}</p>
                <button class="small" onclick="openEndRideModal('${ride.id}')">End Ride</button>
            `;
            activeContainer.appendChild(activeDiv);
        }
    });

    if (!hasActive) {
        activeContainer.innerHTML = '<p>No active rides.</p>';
    }
}

async function startRide() {
    const userId = document.getElementById('userId').value;
    if (!userId) {
        alert('Please enter a User ID');
        return;
    }

    try {
        const response = await fetch('/api/rides/start', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId })
        });

        if (response.ok) {
            document.getElementById('userId').value = '';
            fetchRides();
        } else {
            alert('Failed to start ride');
        }
    } catch (error) {
        console.error('Error starting ride:', error);
    }
}

function openEndRideModal(rideId) {
    selectedRideId = rideId;
    document.getElementById('modalRideIdText').innerText = 'Ride ID: ' + rideId;
    document.getElementById('endRideModal').style.display = 'block';
}

function closeModal() {
    document.getElementById('endRideModal').style.display = 'none';
    selectedRideId = null;
}

async function confirmEndRide() {
    const distance = document.getElementById('distance').value;
    if (!distance || distance <= 0) {
        alert('Please enter a valid distance');
        return;
    }

    try {
        const response = await fetch('/api/rides/end', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ 
                rideId: selectedRideId,
                distance: parseFloat(distance)
            })
        });

        if (response.ok) {
            closeModal();
            fetchRides();
        } else {
            const err = await response.text();
            alert('Failed to end ride: ' + err);
        }
    } catch (error) {
        console.error('Error ending ride:', error);
    }
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    // Handle array format from Jackson if it happens, or string
    if (Array.isArray(dateStr)) {
        const [y, m, d, h, min] = dateStr;
        return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}`;
    }
    const d = new Date(dateStr);
    return d.toLocaleString();
}

// Initial fetch
fetchRides();
// Refresh every 10 seconds
setInterval(fetchRides, 10000);
