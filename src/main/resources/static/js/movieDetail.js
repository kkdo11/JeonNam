function initMap() {
    const mapContainer = document.getElementById('map-area');

    if (!mapContainer) {
        console.error("map-area 요소를 찾을 수 없습니다.");
        return;
    }

    const lat = parseFloat(mapContainer.dataset.latitude);
    const lon = parseFloat(mapContainer.dataset.longitude);

    if (isNaN(lat) || isNaN(lon)) {
        console.error("지도 초기화 실패: 유효한 좌표가 없습니다.");
        return;
    }

    const mapOption = {
        center: new kakao.maps.LatLng(lat, lon),
        level: 4
    };

    const map = new kakao.maps.Map(mapContainer, mapOption);

    const marker = new kakao.maps.Marker({
        map: map,
        position: new kakao.maps.LatLng(lat, lon),
        title: "촬영지"
    });

    const zoomControl = new kakao.maps.ZoomControl();
    map.addControl(zoomControl, kakao.maps.ControlPosition.RIGHT);
}
