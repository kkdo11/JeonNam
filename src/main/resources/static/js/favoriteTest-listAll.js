function initMap() {

    console.log("✅ initMap() 시작됨");
    console.log("📦 favoriteList 전체:", favoriteList);
    const mapContainer = document.getElementById('map-area');

    if (!mapContainer) {
        console.error("map-area 요소를 찾을 수 없습니다.");
        return;
    }

    if (!favoriteList || favoriteList.length === 0) {
        console.error("favoriteList가 비어 있습니다.");
        return;
    }

    const mapOption = {
        center: new kakao.maps.LatLng(34.5, 126.9), // 임시 중심
        level: 10
    };

    const map = new kakao.maps.Map(mapContainer, mapOption);
    const bounds = new kakao.maps.LatLngBounds();

    // 줌 컨트롤 추가
    const zoomControl = new kakao.maps.ZoomControl();
    map.addControl(zoomControl, kakao.maps.ControlPosition.RIGHT);

    // ✅ 이전에 열려 있던 인포윈도우 추적
    let openedInfoWindow = null;

    favoriteList.forEach(item => {
        const lat = parseFloat(item.y);
        const lon = parseFloat(item.x);

        if (isNaN(lat) || isNaN(lon)) return;

        const position = new kakao.maps.LatLng(lat, lon);

        // ✅ bounds는 movie 마커만 반영
        // if (item.type === 'media') {
        //     bounds.extend(position);
        // }

            bounds.extend(position);


        let markerImage;

        if (item.type === 'media') {
            markerImage = new kakao.maps.MarkerImage(
                '/images/JN_marker1.png',
                new kakao.maps.Size(40, 60) // 🔺 tall marker
            );
        } else {
            markerImage = new kakao.maps.MarkerImage(
                '/images/marker/favorite_marker.png',
                new kakao.maps.Size(60, 60) // ✅ square marker
            );
        }

        // ✅ 마커 생성
        const marker = new kakao.maps.Marker({
            map: map,
            position: position,
            title: item.name,
            image: markerImage
        });

        // ✅ 인포윈도우 생성
        const infoWindow = new kakao.maps.InfoWindow({
            content: `<div style="padding:5px;">${item.name}</div>`
        });

        // ✅ 클릭 이벤트
        kakao.maps.event.addListener(marker, 'click', () => {
            if (openedInfoWindow) {
                openedInfoWindow.close();
            }
            infoWindow.open(map, marker);
            openedInfoWindow = infoWindow;
        });
    });


    map.setBounds(bounds); // 일단 전체 마커 기준 bounds 적용

    const sw = bounds.getSouthWest();
    const ne = bounds.getNorthEast();

    const latMin = sw.getLat();
    const latMax = ne.getLat();
    const lngMin = sw.getLng();
    const lngMax = ne.getLng();


// 중심 위도 기반으로 경도 1도당 거리 계산
    const centerLat = (sw.getLat() + ne.getLat()) / 2;
    const latPerKm = 1 / 111;
    const lngPerKm = 1 / (111 * Math.cos(centerLat * Math.PI / 180));

// 반경 3km만큼 안쪽으로 축소
    const kmRadius = 3;
    const latOffset = latPerKm * kmRadius;
    const lngOffset = lngPerKm * kmRadius;

    const newSw = new kakao.maps.LatLng(sw.getLat() + latOffset, sw.getLng() + lngOffset);
    const newNe = new kakao.maps.LatLng(ne.getLat() - latOffset, ne.getLng() - lngOffset);

    const adjustedBounds = new kakao.maps.LatLngBounds();
    adjustedBounds.extend(newSw);
    adjustedBounds.extend(newNe);

// 축소된 bounds 적용
    map.setBounds(adjustedBounds);


// ✅ 일회성 이벤트 리스너 (once 대체 구현)
    const onIdleOnce = () => {
        const currentLevel = map.getLevel();
        if (currentLevel < 5) {
            map.setLevel(6);
        }
        kakao.maps.event.removeListener(map, 'idle', onIdleOnce); // ✅ 한 번만 실행
    };

    kakao.maps.event.addListener(map, 'idle', onIdleOnce);

    window.mapInstance = map;
}

function formatPlanContents(text) {
    if (!text) return '';
    return text
        .replace(/&/g, '&amp;')      // Escape HTML (safety)
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/\n/g, '<br>')      // Preserve line breaks
        .replace(/●/g, "&#9679;")
        .replace(/■/g, '<br><strong>■</strong>'); // Highlight bullets if needed
}

document.addEventListener('DOMContentLoaded', () => {

    // ✅ 저장 버튼 클릭 시 즐겨찾기 저장 요청
    document.addEventListener("click", function (e) {
        const deleteBtn = e.target.closest(".delete-favorite-btn");
        if (deleteBtn) {
            const type = deleteBtn.dataset.type;
            const name = deleteBtn.dataset.name;
            const addr = deleteBtn.dataset.addr;

            if (!confirm(`'${name}' 즐겨찾기를 삭제하시겠습니까?`)) return;

            fetch(`/favorite?type=${encodeURIComponent(type)}&name=${encodeURIComponent(name)}&addr=${encodeURIComponent(addr)}`, {
                method: "DELETE"
            })
                .then(res => {
                    if (res.status === 401) {
                        alert("❌ 로그인 후 삭제할 수 있습니다.");
                        return;
                    } else if (!res.ok) {
                        throw new Error("삭제 실패");
                    }
                    return res.text();
                })
                .then(msg => {
                    alert("✅ 삭제되었습니다.");
                    location.reload();
                })
                .catch(err => {
                    console.error("❌ 삭제 중 오류:", err);
                    alert("❌ 삭제에 실패했습니다.");
                });

            return; // 🛑 stop propagation
        }

        // ✅ 지도 이동 버튼 처리
        const moveBtn = e.target.closest(".move-to-map-btn");
        if (moveBtn) {
            const x = parseFloat(moveBtn.dataset.x);
            const y = parseFloat(moveBtn.dataset.y);

            if (!isNaN(x) && !isNaN(y) && window.mapInstance) {
                const latlng = new kakao.maps.LatLng(y, x);
                window.mapInstance.setCenter(latlng);
            } else {
                console.error("❌ 좌표가 잘못되었거나 mapInstance가 없습니다.");
            }

            return; // stop propagation
        }

        const btn = e.target.closest(".save-favorite-btn"); // ✅ 버튼 내부 아이콘 클릭 대비
        console.log("clicked:", e.target);
        if (!btn) return;



        const payload = {
            type: btn.dataset.type || "theme",
            name: btn.dataset.name,
            location: btn.dataset.address,
            addr: btn.dataset.address || "",
            posterUrl: btn.dataset.posterUrl || "",
            x: parseFloat(btn.dataset.x),
            y: parseFloat(btn.dataset.y),
            planPhone: btn.dataset.phone ?? "",
            planHomepage: btn.dataset.url ?? "",
            planParking: btn.dataset.planParking ?? "",
            planContents: btn.dataset.contents || ""
        };

        console.log("Saving payload:", payload);

        fetch(`/favorite/check?type=theme&name=${encodeURIComponent(btn.dataset.name)}&addr=${encodeURIComponent(btn.dataset.address)}`)
            .then(res => res.json())
            .then(exists => {
                console.log("중복 체크 결과:", exists);
                if (exists) {
                    alert("⚠️ 이미 저장된 항목입니다.");
                    return;
                }

                fetch("/favorite", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify(payload)
                })
                    .then(res => {
                        if (res.status === 401) {
                            alert("❌ 로그인 후 저장할 수 있습니다.");
                            return;
                        }
                        return res.json();
                    })
                    .then(data => {
                        if (data) {
                            alert("✅ 즐겨찾기에 저장되었습니다.");
                            location.reload();
                        }
                    })
                    .catch(err => {
                        console.error("❌ 저장 중 오류:", err);
                        alert("❌ 저장에 실패했습니다.");
                    });
            });
    });
    const nearbyTab = document.getElementById('nearby-tab');

    if (nearbyTab) {
        nearbyTab.addEventListener('shown.bs.tab', function () {
            const map = window.kakao && kakao.maps && window.mapInstance;
            if (!map) return;

            const bounds = map.getBounds();
            const sw = bounds.getSouthWest();
            const ne = bounds.getNorthEast();

            const latMin = sw.getLat();
            const latMax = ne.getLat();
            const lngMin = sw.getLng();
            const lngMax = ne.getLng();

            // AJAX 요청
            fetch(`/favorite/nearby?latMin=${latMin}&latMax=${latMax}&lngMin=${lngMin}&lngMax=${lngMax}`)
                .then(res => res.json())
                .then(data => {
                    const container = document.getElementById('nearby-result');
                    container.innerHTML = '';

                    if (data.length === 0) {
                        container.innerHTML = '<p>추천 관광지가 없습니다.</p>';
                        return;
                    }

                    data.forEach(item => {
                        const hasPoster = item.posterUrl && item.posterUrl.trim() !== "";
                        const card = document.createElement('div');
                        card.className = 'card shadow-sm mb-3';
                        const row = document.createElement('div');
                        row.className = 'row no-gutters';

                        if (hasPoster) {
                            const imgCol = document.createElement('div');
                            imgCol.className = 'col-md-4';
                            const img = document.createElement('img');
                            img.src = item.posterUrl;
                            img.alt = item.planName;
                            img.className = 'img-fluid rounded-start';
                            imgCol.appendChild(img);
                            row.appendChild(imgCol);
                        }

                        const textCol = document.createElement('div');
                        textCol.className = hasPoster ? 'col-md-8' : 'col-12';

                        const body = document.createElement('div');
                        body.className = 'card-body';

                        body.innerHTML = `
                        <h5 class="card-title">${item.planName || '제목 없음'}</h5>
                        <p class="card-text">주소: ${item.planAddr || '정보 없음'}</p>
                        ${item.planPhone ? `<p class="card-text">전화: ${item.planPhone}</p>` : ''}
                        ${item.planHomepage ? `<p><a href="${item.planHomepage}" target="_blank">홈페이지 방문</a></p>` : ''}
                        ${item.planContents ? `<p class="card-text">${formatPlanContents(item.planContents)}</p>` : ''}
                    `;

                        const btnWrapper = document.createElement('div');
                        btnWrapper.className = 'text-end';

                        const saveBtn = document.createElement('button');
                        saveBtn.className = 'btn btn-sm btn-warning save-favorite-btn';
                        saveBtn.innerText = '저장';
                        saveBtn.dataset.name = item.planName || '';
                        saveBtn.dataset.address = item.planAddr || '';
                        saveBtn.dataset.phone = item.planPhone || '';
                        saveBtn.dataset.url = item.planHomepage || '';
                        saveBtn.dataset.x = item.planLongitude;
                        saveBtn.dataset.y = item.planLatitude;
                        saveBtn.dataset.type = 'theme';
                        saveBtn.dataset.posterUrl = item.posterUrl || '';
                        saveBtn.dataset.contents = item.planContents || '';
                        saveBtn.dataset.planParking = item.planParking || '';


                        const moveBtn = document.createElement('button');
                        moveBtn.className = 'btn btn-sm btn-outline-secondary move-to-map-btn me-2'; // me-2로 간격
                        moveBtn.innerText = ' 지도로 이동';
                        moveBtn.dataset.x = item.planLongitude;
                        moveBtn.dataset.y = item.planLatitude;

                        btnWrapper.appendChild(moveBtn); // 위치 버튼 먼저

                        btnWrapper.appendChild(saveBtn);
                        body.appendChild(btnWrapper);
                        textCol.appendChild(body);
                        row.appendChild(textCol);
                        card.appendChild(row);
                        container.appendChild(card);

                        // 마커
                        const lat = parseFloat(item.planLatitude);
                        const lon = parseFloat(item.planLongitude);

                        if (!isNaN(lat) && !isNaN(lon)) {
                            const position = new kakao.maps.LatLng(lat, lon);
                            const markerImage = new kakao.maps.MarkerImage(
                                '/images/marker/recommend_marker.png',  // ✅ your custom marker path
                                new kakao.maps.Size(40, 60)             // ✅ adjust size as needed
                            );

                            const marker = new kakao.maps.Marker({
                                map: window.mapInstance,
                                position: position,
                                title: item.planName,
                                image: markerImage                       // ✅ custom image applied
                            });

                            const infoWindow = new kakao.maps.InfoWindow({
                                content: `<div style="padding:5px;">${item.planName}</div>`
                            });

                            kakao.maps.event.addListener(marker, 'click', () => {
                                infoWindow.open(window.mapInstance, marker);
                            });
                        }
                    });
                })
                .catch(err => {
                    document.getElementById('nearby-result').innerHTML = '<p>데이터를 불러오는 중 오류가 발생했습니다.</p>';
                    console.error(err);
                });
        });
    }


    const searchTourBtn = document.getElementById("searchTourBtn");
    if (searchTourBtn) {
        searchTourBtn.addEventListener("click", function () {
            const form = document.getElementById("searchForm");
            const keyword = form.tourName.value.trim();

            if (keyword === "") {
                alert("⚠️ 관광지 이름을 입력하세요.");
                form.tourName.focus();
                return;
            }

            fetch("/favorite/searchTour", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({tName: keyword}) // ✅ Keyword field updated
            })
                .then(res => res.json())
                .then(json => {
                    const rList = json.data ?? json; // ✅ Handle plain list fallback
                    const rCard = document.getElementById("searchResultCard");
                    const rListElement = document.getElementById("searchResultList");

                    rListElement.innerHTML = "";
                    if (window.searchMarkers && Array.isArray(window.searchMarkers)) {
                        window.searchMarkers.forEach(m => m.setMap(null));
                    }
                    window.searchMarkers = [];

                    if (!rList || rList.length === 0) {
                        rListElement.innerHTML = `
                                                    <div class="card shadow-sm">
                                                        <div class="card-body text-center">
                                                            <p class="card-text text-muted mb-0">검색 결과가 없습니다.</p>
                                                        </div>
                                                    </div>
                                                `;
                    } else {
                        rList.forEach(tour => {
                            const html = `
                            <div class="card shadow-sm mb-3">
                                <div class="card-body">
                                    <h5 class="card-title">${tour.name}</h5>
                                    <p class="card-subtitle mb-2 text-muted">유형: 관광지</p>
                                    ${tour.address ? `<p class="card-text">주소: ${tour.address}</p>` : ''}
                                    ${tour.phone ? `<p class="card-text">전화: ${tour.phone}</p>` : ''}
                                    ${tour.url ? `<p class="card-text"><a href="${tour.url}" target="_blank">홈페이지 방문</a></p>` : ''}
                                    <div class="text-end">
                                        <button class="btn btn-sm btn-outline-secondary move-to-map-btn"
                                            data-x="${tour.x}" data-y="${tour.y}">
                                            지도이동
                                        </button>
                                        <button class="btn btn-sm btn-warning save-favorite-btn"
                                            data-type="theme"
                                            data-name="${tour.name}"
                                            data-address="${tour.address || ''}"
                                            data-phone="${tour.phone || ''}"
                                            data-url="${tour.url || ''}"
                                            data-x="${tour.x}" data-y="${tour.y}"
                                            data-poster-url="${tour.posterUrl || ''}"
                                            data-contents="${(tour.contents || '').replace(/"/g, '&quot;')}">
                                            저장
                                        </button>
                                    </div>
                                </div>
                            </div>
                        `;

                            rListElement.innerHTML += html;

                            // Marker
                            const markerImage = new kakao.maps.MarkerImage(
                                '/images/marker/search_marker.png',
                                new kakao.maps.Size(40, 60)
                            );

                            const marker = new kakao.maps.Marker({
                                map: window.mapInstance,
                                position: new kakao.maps.LatLng(tour.y, tour.x),
                                image: markerImage
                            });

                            window.searchMarkers.push(marker);
                        });



                        // 지도 이동 버튼 동작
                        document.querySelectorAll(".move-to-map-btn").forEach(button => {
                            button.addEventListener("click", function () {
                                const x = parseFloat(this.dataset.x);
                                const y = parseFloat(this.dataset.y);
                                const latlng = new kakao.maps.LatLng(y, x);
                                window.mapInstance.setCenter(latlng);
                            });
                        });
                    }

                    rCard.style.display = "block";
                })
                .catch(() => {
                    alert("❌ 관광지 검색 중 문제가 발생했습니다.");
                });
        });
    }


});






