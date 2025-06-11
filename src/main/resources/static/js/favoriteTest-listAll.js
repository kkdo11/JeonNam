function initMap() {
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
        if (item.type === 'movie') {
            bounds.extend(position);
        }

        // ✅ 마커 이미지 설정
        const markerImage = new kakao.maps.MarkerImage(
            item.type === 'movie'
                ? '/images/movie_marker.png'
                : '/images/course_marker.png',
            new kakao.maps.Size(40, 40)
        );

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

}
