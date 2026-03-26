document.addEventListener('DOMContentLoaded', () => {
    const findBtn = document.getElementById('findBtn');
    const statusMessage = document.getElementById('statusMessage');
    const btnText = findBtn.querySelector('.btn-text');
    const loader = findBtn.querySelector('.loader');
    const resultsContainer = document.getElementById('resultsContainer');
    const pharmacyList = document.getElementById('pharmacyList');

    findBtn.addEventListener('click', () => {
        if (!navigator.geolocation) {
            showStatus('Tarayıcınız konum servisini desteklemiyor.', 'error');
            return;
        }

        setLoading(true);
        showStatus('Konumunuz bulunuyor...', 'success');

        navigator.geolocation.getCurrentPosition(
            position => {
                const lat = position.coords.latitude;
                const lon = position.coords.longitude;
                const now = new Date();
                const offset = -now.getTimezoneOffset();
                const diff = offset >= 0 ? '+' : '-';
                const pad = (num) => (num < 10 ? '0' : '') + num;
                const time = now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate()) +
                    'T' + pad(now.getHours()) + ':' + pad(now.getMinutes()) + ':' + pad(now.getSeconds()) +
                    diff + pad(Math.floor(Math.abs(offset) / 60)) + ':' + pad(Math.abs(offset) % 60);

                showStatus('Eczaneler aranıyor...', 'success');
                fetchPharmacies(lat, lon, time);
            },
            error => {
                setLoading(false);
                let msg = 'Konum alınamadı.';
                if (error.code === 1) msg = 'Konum izni reddedildi. Lütfen tarayıcı ayarlarından izin verin.';
                else if (error.code === 2) msg = 'Konum belirlenemedi.';
                else if (error.code === 3) msg = 'Konum isteği zaman aşımına uğradı.';
                showStatus(msg, 'error');
            },
            { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
        );
    });

    async function fetchPharmacies(lat, lon, time) {
        try {
            const response = await fetch(`/pharmacies?lat=${lat}&lon=${lon}&time=${encodeURIComponent(time)}`);
            if (!response.ok) {
                throw new Error('Sunucu hatası veya eczane bulunamadı');
            }

            const data = await response.json();
            displayResults(data.pharmacies);
            showStatus('', 'success');
        } catch (error) {
            showStatus('Eczaneler getirilirken bir hata oluştu: ' + error.message, 'error');
        } finally {
            setLoading(false);
        }
    }

    function displayResults(pharmacies) {
        console.log("Gelen veriler:", pharmacies); // Hata ayıklama için ekledik
        pharmacyList.innerHTML = '';

        if (!pharmacies || !Array.isArray(pharmacies) || pharmacies.length === 0) {
            pharmacyList.innerHTML = '<p style="grid-column: 1/-1; text-align: center; color: var(--text-muted);">Yakınlarınızda açık veya nöbetçi eczane bulunamadı.</p>';
        } else {
            pharmacies.forEach((pharmacy, index) => {
                const delay = index * 0.1;
                const card = document.createElement('div');
                card.className = 'pharmacy-card';
                card.style.animation = `fadeInUp 0.5s ease ${delay}s both`;

                let distanceText = 'Uzaklık hesabı yapılamadı';
                if (pharmacy.distance !== null && pharmacy.distance !== undefined && pharmacy.distance < 1000000) {
                    distanceText = pharmacy.distance >= 1000
                        ? (pharmacy.distance / 1000).toFixed(1) + ' km'
                        : Math.round(pharmacy.distance) + ' m';
                }

                const mapUrl = `https://www.google.com/maps/dir/?api=1&destination=${pharmacy.lat},${pharmacy.lon}`;

                card.innerHTML = `
                    <div class="pharmacy-name">${pharmacy.display_name}</div>
                    <div class="pharmacy-distance">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                            <circle cx="12" cy="10" r="3"></circle>
                        </svg>
                        ${distanceText}
                    </div>
                    ${pharmacy.address ? `<div style="font-size: 0.9rem; margin-top: 0.5rem; color: var(--text-muted);">${pharmacy.address}</div>` : ''}
                    <a href="${mapUrl}" target="_blank" rel="noopener noreferrer" class="map-btn">Yol Tarifi Al</a>
                `;
                pharmacyList.appendChild(card);
            });
        }

        resultsContainer.classList.remove('hidden');
        resultsContainer.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    function setLoading(isLoading) {
        findBtn.disabled = isLoading;
        if (isLoading) {
            btnText.classList.add('hidden');
            loader.classList.remove('hidden');
        } else {
            btnText.classList.remove('hidden');
            loader.classList.add('hidden');
        }
    }

    function showStatus(message, type) {
        statusMessage.textContent = message;
        statusMessage.className = 'status-message ' + (type === 'success' ? 'success' : '');
    }
});
