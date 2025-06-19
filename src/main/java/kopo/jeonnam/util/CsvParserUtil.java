package kopo.jeonnam.util;

import kopo.jeonnam.dto.csv.MediaSpotDTO;
import kopo.jeonnam.dto.csv.ProductDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CsvParserUtil {

    // 제품별 정보 저장용 내부 클래스
    private static class ProductInfo {
        String feature;
        String benefit;
        String imageUrl;

        public ProductInfo(String feature, String benefit, String imageUrl) {
            this.feature = feature;
            this.benefit = benefit;
            this.imageUrl = imageUrl;
        }
    }

    // 미리 맵으로 농산물별 특징, 효능, 이미지 경로 등록
    private static final Map<String, ProductInfo> productInfoMap = new HashMap<>();

    static {
        productInfoMap.put("녹차", new ProductInfo(
                "녹차는 찻잎을 수확한 직후 찌거나 덖는 공정을 통해 산화를 억제하여 신선한 녹색과 향을 유지합니다.<br>" +
                        "쌉싸름하면서도 은은한 단맛, 고소한 향이 특징이며, 카테킨, 테아닌, 카페인, 비타민 C 등의 유익한 성분이 풍부합니다.<br>" +
                        "보성군은 안개와 일조량이 적절한 기후 조건을 갖춰 고품질 녹차 재배에 최적의 환경을 제공합니다.<br>" +
                        "녹차는 건강 음료로서 꾸준한 인기를 끌고 있으며 다양한 요리에 활용됩니다.<br>",

                "카테킨 성분은 강력한 항산화 작용으로 피부 노화 방지와 면역력 강화에 기여합니다.<br>" +
                        "콜레스테롤 조절 및 혈압 안정화에 효과적이며, 체지방 분해를 촉진해 다이어트에도 좋습니다.<br>" +
                        "테아닌과 카페인은 뇌 기능 활성화에 도움을 주어 집중력과 기억력 향상에 유익합니다.<br>" +
                        "비타민 C는 면역력 증진과 피로 회복에도 효과적입니다.<br>",

                "/images/products/green-tea.jpg"
        ));

        productInfoMap.put("겨울배추", new ProductInfo(
                "겨울배추는 추운 겨울에도 잘 자라는 품종으로 단단한 잎과 아삭한 식감을 자랑합니다.<br>" +
                        "해남군의 기후와 토양에서 자라 신선하고 맛이 뛰어납니다.<br>" +
                        "김치 재료로 널리 사용되며 저장성이 좋아 겨울철 식탁에 자주 오릅니다.<br>" +
                        "영양소가 풍부해 다양한 요리에 활용됩니다.<br>",

                "비타민 C와 식이섬유가 풍부하여 면역력 강화와 소화 개선에 도움을 줍니다.<br>" +
                        "칼로리가 낮고 포만감을 높여 다이어트 식품으로도 적합합니다.<br>" +
                        "항산화 물질이 함유되어 노화 방지에도 도움을 줍니다.<br>" +
                        "혈당 조절과 체내 독소 배출에도 긍정적인 효과가 있습니다.<br>",

                "/images/products/winter-cabbage.jpg"
        ));

        productInfoMap.put("유자", new ProductInfo(
                "유자는 독특한 향과 상큼한 맛이 특징인 감귤류 과일입니다.<br>" +
                        "고흥군에서 재배되어 품질이 우수하며, 주로 차나 조미료로 사용됩니다.<br>" +
                        "껍질과 과육 모두 활용도가 높으며 겨울철 비타민 공급원으로 인기가 많습니다.<br>" +
                        "전통 음식과 음료에 다양하게 활용되고 있습니다.<br>",

                "비타민 C가 매우 풍부해 감기 예방과 피부 미용에 효과적입니다.<br>" +
                        "항산화 성분이 면역력 강화와 노화 방지에 도움을 줍니다.<br>" +
                        "플라보노이드가 혈액 순환 개선과 피로 회복에 기여합니다.<br>" +
                        "소화 촉진과 체내 독소 제거에도 효과적입니다.<br>",

                "/images/products/yuja.jpg"
        ));

        productInfoMap.put("홍주", new ProductInfo(
                "진도군에서 생산되는 홍주는 전통 방식으로 제조되는 고유의 증류주입니다.<br>" +
                        "깊고 은은한 향과 깔끔한 맛이 특징이며 지역 특산품으로 유명합니다.<br>" +
                        "엄선된 원료와 숙성 과정을 통해 고품질을 유지합니다.<br>" +
                        "전통주 애호가들 사이에서 높은 평가를 받고 있습니다.<br>",

                "적당한 음용 시 스트레스 완화와 혈액 순환에 도움을 줄 수 있습니다.<br>" +
                        "심신 안정과 긴장 완화에 긍정적인 효과가 있습니다.<br>" +
                        "소량 섭취는 기분 전환과 피로 회복에 기여합니다.<br>" +
                        "과음은 피하고 적절히 즐기는 것이 중요합니다.<br>",

                "/images/products/hongju.jpg"
        ));

        productInfoMap.put("양파", new ProductInfo(
                "무안군에서 재배되는 양파는 당도가 높고 매운맛이 적어 식용으로 인기가 높습니다.<br>" +
                        "단단하고 아삭한 식감이 뛰어나며 신선도가 우수합니다.<br>" +
                        "다양한 요리에 활용되며 저장성과 운송성이 뛰어납니다.<br>" +
                        "무안지역 토양과 기후가 양파 재배에 최적화되어 있습니다.<br>",

                "혈액 순환 개선과 면역력 증진에 효과적이며 항염 작용도 가지고 있습니다.<br>" +
                        "퀘르세틴 등 항산화 물질이 염증 완화와 심혈관 건강 유지에 기여합니다.<br>" +
                        "혈당 조절과 콜레스테롤 감소에도 긍정적인 역할을 합니다.<br>" +
                        "소화를 돕고 전반적인 건강 증진에 도움을 줍니다.<br>",

                "/images/products/onion.jpg"
        ));

        // 매실 (proFeature, proBenefit 데이터 없음 → 임의로 보완)
        productInfoMap.put("매실", new ProductInfo(
                "광양시에서 재배되는 매실은 신선하고 당도 높은 품질로 유명합니다.<br>" +
                        "특유의 새콤달콤한 맛이 있어 주스나 청으로 많이 활용됩니다.<br>" +
                        "강한 산미와 풍부한 향기가 특징이며, 여름철 대표적인 건강 식품입니다.<br>" +
                        "광양의 기후와 토양은 매실 재배에 최적의 환경을 제공합니다.<br>",

                "매실에는 소화를 돕는 유기산과 항균 성분이 함유되어 소화 개선에 도움을 줍니다.<br>" +
                        "피로 회복과 면역력 강화에 효과적이며 해독 작용도 뛰어납니다.<br>" +
                        "항산화 물질이 노화 방지와 피부 건강 유지에 기여합니다.<br>" +
                        "비타민과 미네랄도 풍부해 전반적인 건강 증진에 도움을 줍니다.<br>",

                "/images/products/plum.jpg"
        ));

        // 고구마 (proFeature, proBenefit 데이터 없음 → 임의로 보완)
        productInfoMap.put("고구마", new ProductInfo(
                "해남군에서 생산되는 고구마는 달콤한 맛과 부드러운 식감이 특징입니다.<br>" +
                        "영양이 풍부하며 저장성이 좋아 가을과 겨울철 대표 간식으로 인기가 많습니다.<br>" +
                        "다양한 요리와 간식 재료로 활용되며 건강식으로 널리 알려져 있습니다.<br>" +
                        "해남의 토양과 기후는 고구마 재배에 이상적인 조건을 갖추고 있습니다.<br>",

                "식이섬유가 풍부하여 소화를 돕고 변비 예방에 효과적입니다.<br>" +
                        "비타민 A, C 및 항산화 물질이 면역력 강화와 피부 건강에 도움을 줍니다.<br>" +
                        "저칼로리 식품으로 다이어트에도 적합하며 혈당 조절에 긍정적 영향을 미칩니다.<br>" +
                        "풍부한 미네랄과 에너지원으로 체력 보충에도 좋습니다.<br>",

                "/images/products/sweet-potato.jpg"
        ));

        // 무화과 (proFeature, proBenefit 데이터 없음 → 임의로 보완)
        productInfoMap.put("무화과", new ProductInfo(
                "영암군에서 재배되는 무화과는 달콤하고 부드러운 과육이 특징입니다.<br>" +
                        "풍부한 수분과 당분으로 건강 간식으로 인기가 많습니다.<br>" +
                        "생과일로 먹거나 건조하여 다양한 음식에 활용됩니다.<br>" +
                        "영암의 자연 환경이 무화과 재배에 적합합니다.<br>",

                "식이섬유가 풍부하여 소화 촉진과 변비 예방에 도움을 줍니다.<br>" +
                        "칼슘과 칼륨이 풍부해 뼈 건강과 혈압 조절에 긍정적인 영향을 미칩니다.<br>" +
                        "항산화 물질이 면역력 증진과 노화 방지에 기여합니다.<br>" +
                        "비타민과 미네랄이 전반적인 건강 증진을 돕습니다.<br>",

                "/images/products/fig.jpg"
        ));

        // 한우 (함평군, 영광군, 고흥군 3곳 중 대표적으로 함평군 한우로)
        productInfoMap.put("한우", new ProductInfo(
                "함평군을 비롯한 전남 지역에서 사육되는 한우는 뛰어난 육질과 풍부한 맛이 특징입니다.<br>" +
                        "철저한 사육 관리와 청정 환경에서 자란 고급 육류로 소비자들의 신뢰를 받고 있습니다.<br>" +
                        "특유의 부드럽고 고소한 맛이 요리의 풍미를 더해 줍니다.<br>" +
                        "고단백, 저지방 식품으로 건강에도 좋은 고급 식재료입니다.<br>",

                "한우는 고단백 식품으로 근육 생성과 회복에 도움을 줍니다.<br>" +
                        "풍부한 철분과 아연은 빈혈 예방과 면역력 강화에 기여합니다.<br>" +
                        "비타민 B군은 에너지 대사와 신경 기능 유지에 필수적입니다.<br>" +
                        "균형 잡힌 영양소로 체력 증진과 건강 유지에 도움을 줍니다.<br>",

                "/images/products/korean-beef.jpg"
        ));

        // 대파 (proFeature, proBenefit 데이터 없음 → 임의로 보완)
        productInfoMap.put("대파", new ProductInfo(
                "진도군에서 재배되는 대파는 신선하고 향이 강한 품질 좋은 채소입니다.<br>" +
                        "요리에 감칠맛과 풍미를 더하는 중요한 부재료로 쓰입니다.<br>" +
                        "진도 지역의 토양과 기후가 대파 재배에 적합한 조건을 제공합니다.<br>" +
                        "비타민과 무기질이 풍부하여 건강에도 좋은 식재료입니다.<br>",

                "대파는 항염 및 항균 작용으로 감기 예방에 도움을 줍니다.<br>" +
                        "소화 촉진과 혈액 순환 개선에 효과적입니다.<br>" +
                        "비타민 C와 알리신 성분이 면역력 강화와 피로 회복에 기여합니다.<br>" +
                        "심혈관 건강 유지에도 긍정적인 영향을 미칩니다.<br>",

                "/images/products/green-onion.jpg"
        ));

        // 찰쌀보리쌀 (proFeature, proBenefit 데이터 없음 → 임의로 보완)
        productInfoMap.put("찰쌀보리쌀", new ProductInfo(
                "영광군에서 생산되는 찰쌀보리쌀은 찰기가 뛰어나고 고소한 맛이 특징입니다.<br>" +
                        "밥에 섞어 먹으면 식감이 부드러워 다양한 요리에 활용됩니다.<br>" +
                        "영광의 토양과 기후가 보리 재배에 최적의 환경을 제공합니다.<br>" +
                        "영양소가 풍부한 건강 곡물로 알려져 있습니다.<br>",

                "찰쌀보리쌀은 식이섬유가 풍부해 장 건강과 변비 예방에 효과적입니다.<br>" +
                        "혈당 조절과 콜레스테롤 개선에 도움을 줍니다.<br>" +
                        "풍부한 미네랄과 비타민이 신진대사 활성화에 기여합니다.<br>" +
                        "다이어트 식품으로도 인기가 높습니다.<br>",

                "/images/products/barley-rice.jpg"
        ));

        // 돌산갓 (proFeature, proBenefit 데이터 없음 → 임의로 보완)
        productInfoMap.put("돌산갓", new ProductInfo(
                "여수시 돌산 지역에서 재배되는 갓으로 매운맛과 특유의 향이 뛰어납니다.<br>" +
                        "김치 재료로 유명하며 신선한 식감과 풍부한 영양을 자랑합니다.<br>" +
                        "여수의 해풍과 토양이 고품질 돌산갓 생산에 기여합니다.<br>" +
                        "전통 식문화에서 중요한 역할을 하고 있습니다.<br>",

                "돌산갓은 비타민 C가 풍부해 면역력 강화에 도움을 줍니다.<br>" +
                        "항산화 물질이 노화 방지와 피로 회복에 기여합니다.<br>" +
                        "소화를 돕고 해독 작용에도 긍정적인 효과가 있습니다.<br>" +
                        "항염 작용으로 염증 완화에 도움을 줍니다.<br>",

                "/images/products/dolsan-gat.jpg"
        ));

        // 돌산갓김치 (proFeature, proBenefit 데이터 없음 → 임의로 보완)
        productInfoMap.put("돌산갓김치", new ProductInfo(
                "여수시 돌산 지역의 신선한 갓으로 만든 김치로 풍부한 향과 매콤한 맛이 특징입니다.<br>" +
                        "전통 방식으로 만들어져 깊은 맛과 아삭한 식감을 자랑합니다.<br>" +
                        "지역 특산품으로 국내외에서 큰 사랑을 받고 있습니다.<br>" +
                        "김치 발효로 인한 유산균이 풍부해 건강에도 좋습니다.<br>",

                "프로바이오틱스가 풍부하여 장 건강과 면역력 증진에 효과적입니다.<br>" +
                        "비타민과 미네랄이 풍부해 피로 회복과 신진대사에 도움을 줍니다.<br>" +
                        "소화를 돕고 항산화 작용으로 노화 방지에 기여합니다.<br>" +
                        "저염식으로 건강한 식단에 적합합니다.<br>",

                "/images/products/dolsan-gat-kimchi.jpg"
        ));

        // 딸기 (proFeature, proBenefit 데이터 없음 → 임의로 보완)
        productInfoMap.put("딸기", new ProductInfo(
                "담양군에서 재배되는 딸기는 당도가 높고 과육이 부드럽고 신선합니다.<br>" +
                        "다양한 품종이 있어 계절별로 맛과 향이 다채롭습니다.<br>" +
                        "신선도가 뛰어나 생과일과 가공품 모두에 인기가 많습니다.<br>" +
                        "담양의 청정 환경이 딸기 재배에 최적의 조건을 제공합니다.<br>",

                "비타민 C와 항산화 물질이 풍부하여 면역력 강화와 피부 건강에 좋습니다.<br>" +
                        "식이섬유가 소화 건강에 도움을 주며 체중 관리에도 효과적입니다.<br>" +
                        "혈액 순환 개선과 피로 회복에도 기여합니다.<br>" +
                        "칼로리가 낮아 다이어트 간식으로 적합합니다.<br>",

                "/images/products/strawberry.jpg"
        ));

        // 웅치올벼쌀 (proFeature, proBenefit 데이터 없음 → 임의로 보완)
        productInfoMap.put("웅치올벼쌀", new ProductInfo(
                "보성군 웅치 지역에서 재배되는 올벼쌀로 고품질과 맛이 뛰어납니다.<br>" +
                        "윤기 있고 찰진 식감이 특징이며 밥맛이 우수합니다.<br>" +
                        "농약 사용을 최소화한 친환경 재배로 건강에도 좋습니다.<br>" +
                        "지역 특성에 맞춘 재배법으로 우수한 품질을 유지합니다.<br>",

                "찰진 식감과 풍부한 영양소가 건강한 에너지원이 됩니다.<br>" +
                        "비타민과 미네랄이 균형 있게 포함되어 신진대사 활성화에 도움을 줍니다.<br>" +
                        "식이섬유가 장 건강과 소화에 긍정적인 영향을 미칩니다.<br>" +
                        "저지방, 저칼로리로 다이어트 식품으로도 적합합니다.<br>",

                "/images/products/ungchi-rice.jpg"
        ));

        // 배 (proFeature, proBenefit 데이터 없음 → 임의로 보완)
        productInfoMap.put("배", new ProductInfo(
                "나주시에서 재배되는 배는 아삭하고 달콤한 맛이 특징입니다.<br>" +
                        "수분 함량이 높아 신선한 식감과 청량감을 제공합니다.<br>" +
                        "주로 생과일로 소비되며 즙으로도 활용됩니다.<br>" +
                        "풍부한 영양소로 여름철 건강 간식으로 인기가 많습니다.<br>",

                "비타민 C와 식이섬유가 풍부해 면역력 강화와 소화 촉진에 도움을 줍니다.<br>" +
                        "항산화 물질이 피부 건강과 노화 방지에 기여합니다.<br>" +
                        "수분 함량이 많아 체내 수분 균형 유지에 도움을 줍니다.<br>" +
                        "칼로리가 낮아 다이어트 식품으로도 적합합니다.<br>",

                "/images/products/pear.jpg"
        ));

        productInfoMap.put("검정쌀", new ProductInfo(
                "진도군에서 생산되는 검정쌀은 깊은 흑색을 띠며 항산화 물질이 풍부한 건강 기능성 쌀입니다.<br>" +
                        "특유의 고소한 맛과 쫄깃한 식감이 특징이며 밥맛이 뛰어납니다.<br>" +
                        "친환경 재배법으로 재배되어 안전성과 품질이 우수합니다.<br>" +
                        "건강을 중시하는 소비자들 사이에서 점점 인기를 얻고 있습니다.<br>",

                "안토시아닌 성분이 풍부해 항산화 효과와 면역력 강화에 도움을 줍니다.<br>" +
                        "혈액 순환 개선과 혈관 건강 유지에 긍정적인 영향을 미칩니다.<br>" +
                        "혈당 조절과 체지방 감소에도 효과적이며 다이어트 식품으로 적합합니다.<br>" +
                        "소화가 잘 되어 장 건강 증진에도 기여합니다.<br>",

                "/images/products/black-rice.jpg"
        ));

        productInfoMap.put("거문도쑥", new ProductInfo(
                "여수시 거문도에서 자라는 쑥으로 향이 강하고 잎이 부드러운 것이 특징입니다.<br>" +
                        "해풍의 영향을 받아 특유의 신선함과 영양가를 지니고 있습니다.<br>" +
                        "전통적으로 약용 및 식용으로 널리 사용되어 왔습니다.<br>" +
                        "쑥 특유의 쌉싸름한 맛과 향이 음식의 풍미를 더해 줍니다.<br>",

                "항염, 항산화 작용으로 면역력 강화에 도움을 줍니다.<br>" +
                        "소화를 촉진하고 체내 독소 제거에 효과적입니다.<br>" +
                        "피로 회복과 혈액 순환 개선에도 긍정적인 역할을 합니다.<br>" +
                        "여성 건강과 피부 미용에도 유익한 성분을 함유하고 있습니다.<br>",

                "/images/products/geomundo-mugwort.jpg"
        ));

        productInfoMap.put("고추", new ProductInfo(
                "영광군에서 재배되는 고추는 색이 선명하고 매운맛이 강렬한 품질 좋은 고추입니다.<br>" +
                        "생산 과정에서 엄격한 품질 관리를 거쳐 신선함을 유지합니다.<br>" +
                        "한국 요리에 빠질 수 없는 중요한 양념 재료로 활용됩니다.<br>" +
                        "다양한 요리에서 매운맛과 풍미를 더하는 역할을 합니다.<br>",

                "캡사이신 성분이 신진대사를 촉진하고 지방 분해를 돕습니다.<br>" +
                        "항염 및 항산화 작용으로 면역력 강화에 기여합니다.<br>" +
                        "통증 완화와 혈액 순환 개선에 도움을 줍니다.<br>" +
                        "소화를 촉진하고 위 건강 유지에도 긍정적인 효과가 있습니다.<br>",

                "/images/products/chili-pepper.jpg"
        ));

        productInfoMap.put("고춧가루", new ProductInfo(
                "영광군에서 생산된 고춧가루는 선명한 붉은색과 고운 입자가 특징입니다.<br>" +
                        "전통적인 방식으로 건조 및 분쇄하여 풍미가 뛰어납니다.<br>" +
                        "김치, 찌개, 양념 등 다양한 요리에 필수적인 재료입니다.<br>" +
                        "신선한 고춧가루는 요리의 맛과 향을 한층 살려줍니다.<br>",

                "캡사이신과 비타민 A가 풍부해 면역력 강화와 시력 보호에 도움을 줍니다.<br>" +
                        "혈액 순환 개선과 항산화 작용으로 건강 유지에 효과적입니다.<br>" +
                        "체내 지방 분해와 신진대사 촉진에도 기여합니다.<br>" +
                        "소화 기능 강화와 피로 회복에 도움을 줍니다.<br>",

                "/images/products/chili-powder.jpg"
        ));

        productInfoMap.put("석류", new ProductInfo(
                "고흥군에서 재배되는 석류는 달콤하면서도 상큼한 맛과 붉은 색이 특징입니다.<br>" +
                        "과육이 풍부하고 즙이 많아 건강식품으로 인기가 높습니다.<br>" +
                        "비타민과 항산화 물질이 많이 함유되어 면역력 강화에 좋습니다.<br>" +
                        "생과일은 물론 주스, 잼, 화장품 원료로도 활용됩니다.<br>",

                "강력한 항산화 작용으로 노화 방지와 피부 건강에 효과적입니다.<br>" +
                        "혈압 조절과 심혈관 건강 유지에 도움을 줍니다.<br>" +
                        "항염 작용으로 염증 완화에 기여하며 면역 체계 강화에 유익합니다.<br>" +
                        "소화 촉진과 체내 독소 배출에도 도움을 줍니다.<br>",

                "/images/products/pomegranate.jpg"
        ));

        productInfoMap.put("울금", new ProductInfo(
                "진도군에서 재배되는 울금은 생강과에 속하는 향신료로 강한 노란색을 띕니다.<br>" +
                        "전통 한방 재료로 사용되며 건강 기능성이 뛰어납니다.<br>" +
                        "울금은 독특한 향과 쓴맛을 가지고 있어 요리에 풍미를 더합니다.<br>" +
                        "최근 건강식품으로도 주목받고 있습니다.<br>",

                "커큐민 성분이 강력한 항염 및 항산화 효과를 제공합니다.<br>" +
                        "소화 촉진과 간 기능 개선에 도움을 줍니다.<br>" +
                        "관절염 및 만성 염증 완화에 효과적입니다.<br>" +
                        "면역력 강화와 암 예방에도 긍정적인 역할을 합니다.<br>",

                "/images/products/turmeric.jpg"
        ));

        productInfoMap.put("마늘", new ProductInfo(
                "고흥군에서 재배되는 마늘은 알이 굵고 풍미가 진한 것이 특징입니다.<br>" +
                        "전통적인 재배 방식과 청정 환경에서 생산되어 품질이 우수합니다.<br>" +
                        "요리의 기본 재료로 널리 사용되며 건강식품으로 인기가 높습니다.<br>" +
                        "마늘 특유의 향과 맛이 음식에 깊이를 더합니다.<br>",

                "알리신 성분이 항균 및 항바이러스 효과를 발휘합니다.<br>" +
                        "혈액 순환 개선과 콜레스테롤 조절에 도움을 줍니다.<br>" +
                        "면역력 강화와 피로 회복에 효과적입니다.<br>" +
                        "심혈관 건강 유지와 암 예방에도 긍정적인 영향을 미칩니다.<br>",

                "/images/products/garlic.jpg"
        ));

        productInfoMap.put("모싯잎송편", new ProductInfo(
                "영광군에서 생산되는 모싯잎송편은 쫄깃한 식감과 은은한 모싯잎 향이 특징입니다.<br>" +
                        "전통 방식으로 만들어져 명절과 행사에 많이 소비됩니다.<br>" +
                        "모싯잎의 자연 향과 색감이 송편에 특별한 맛을 부여합니다.<br>" +
                        "지역 특산품으로 품질이 뛰어나 전국적으로 알려져 있습니다.<br>",

                "모싯잎에 함유된 항산화 물질이 건강 증진에 도움을 줍니다.<br>" +
                        "식이섬유가 풍부하여 소화 개선과 변비 예방에 효과적입니다.<br>" +
                        "비타민과 미네랄이 풍부해 전반적인 면역력 강화에 기여합니다.<br>" +
                        "천연 재료로 알레르기 위험이 적어 안전한 식품입니다.<br>",

                "/images/products/mosip-leaf-rice-cake.jpg"
        ));

        productInfoMap.put("토란", new ProductInfo(
                "곡성군에서 재배되는 토란은 부드럽고 담백한 맛과 질감이 특징입니다.<br>" +
                        "국물 요리나 찜 요리에 주로 활용되며 영양가가 높습니다.<br>" +
                        "섬유질과 전분이 풍부해 소화에 도움이 됩니다.<br>" +
                        "곡성 지역의 기후와 토양이 토란 재배에 적합합니다.<br>",

                "식이섬유가 풍부해 장 건강과 변비 예방에 효과적입니다.<br>" +
                        "칼륨 함량이 높아 혈압 조절에 도움을 줍니다.<br>" +
                        "비타민 C와 항산화 물질이 면역력 강화에 기여합니다.<br>" +
                        "피로 회복과 체내 독소 배출에도 긍정적인 영향을 미칩니다.<br>",

                "/images/products/taro.jpg"
        ));

        productInfoMap.put("키위", new ProductInfo(
                "보성군에서 재배되는 키위는 당도가 높고 신선하며 과육이 부드럽습니다.<br>" +
                        "비타민과 미네랄 함량이 풍부해 건강 간식으로 각광받고 있습니다.<br>" +
                        "생과일로 먹거나 주스로 가공되어 다양하게 소비됩니다.<br>" +
                        "보성의 기후 조건이 키위 재배에 매우 적합합니다.<br>",

                "비타민 C가 풍부해 면역력 강화와 피부 미용에 좋습니다.<br>" +
                        "식이섬유가 소화를 돕고 혈당 조절에도 도움을 줍니다.<br>" +
                        "칼륨 함량이 높아 혈압 조절에 효과적입니다.<br>" +
                        "항산화 성분이 노화 방지와 심혈관 건강 유지에 기여합니다.<br>",

                "/images/products/kiwi.jpg"
        ));

        productInfoMap.put("멜론", new ProductInfo(
                "곡성군에서 재배되는 멜론은 달콤하고 향긋한 맛과 부드러운 과육이 특징입니다.<br>" +
                        "과즙이 풍부하여 여름철 대표 과일로 인기가 많습니다.<br>" +
                        "생과일뿐만 아니라 주스나 디저트 재료로도 활용됩니다.<br>" +
                        "곡성 지역의 온화한 기후가 멜론 재배에 적합합니다.<br>",

                "비타민 A와 C가 풍부해 면역력 강화와 피부 건강에 도움을 줍니다.<br>" +
                        "수분 함량이 높아 체내 수분 유지와 피로 회복에 효과적입니다.<br>" +
                        "식이섬유가 소화를 돕고 변비 예방에도 긍정적입니다.<br>" +
                        "저칼로리 과일로 다이어트 간식으로도 적합합니다.<br>",

                "/images/products/melon.jpg"
        ));
    }



    public static List<ProductDTO> parseProducts(InputStream csvInputStream) {
        Map<String, ProductDTO> productMap = new LinkedHashMap<>(); // 중복 방지용 Map

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(csvInputStream, Charset.forName("MS949")))) {

            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .parse(reader);

            for (CSVRecord record : records) {
                String rawProName = record.get("등록명칭").trim();
                String rawProArea = record.get("대상지역").trim();

                String proName = extractProductName(rawProName);
                String proArea = extractAreaName(rawProArea);

                // 맵에서 정보 조회
                ProductInfo info = productInfoMap.get(proName);

                // 특정 품목은 다중 지역으로 수동 지정
                if ("한우".equals(proName)) {
                    proArea = "함평군, 영광군, 고흥군";
                }

                // 이미 저장된 상품이면 skip
                if (productMap.containsKey(proName)) continue;

                ProductDTO dto = ProductDTO.builder()
                        .proId(null)
                        .proRegNo(record.get("등록번호").trim())
                        .proName(proName)
                        .proRegDate(record.get("등록일자").trim())
                        .proArea(proArea)
                        .proPlanQty(record.get("생산계획량(톤)").trim())
                        .proCompany(record.get("업체명").trim())
                        .proBaseDate(record.get("데이터기준일자").trim())
                        .proFeature(info != null ? info.feature : null)
                        .proBenefit(info != null ? info.benefit : null)
                        .imageUrl(info != null ? info.imageUrl : null)
                        .build();

                productMap.put(proName, dto); // 중복 방지를 위해 map에 저장
            }

            log.info("✅ CSV 파싱 완료, 중복 제거 후 총 {}개 아이템 읽음", productMap.size());

        } catch (Exception e) {
            log.error("❌ CSV 파싱 중 오류 발생", e);
        }

        return new ArrayList<>(productMap.values());
    }

    private static String extractProductName(String rawName) {
        // "보성 녹차" -> "녹차"
        String[] words = rawName.split(" ");
        return words.length > 0 ? words[words.length - 1] : rawName;
    }

    private static String extractAreaName(String rawArea) {
        // "행정구역상 전라남도 보성군 일원" 또는 "전라남도 순천시 일원" → "보성군", "순천시"
        String cleaned = rawArea.replace("행정구역상", "").trim();

        // 군 또는 시를 포함하는 패턴
        Pattern pattern = Pattern.compile("전라남도\\s+(.+?[군시])\\s+일원");
        Matcher matcher = pattern.matcher(cleaned);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return rawArea;
    }


    public static List<MediaSpotDTO> parseMediaSpots(InputStream csvInputStream) {
        List<MediaSpotDTO> list = new ArrayList<>();
        Set<String> spotNameSet = new HashSet<>();  // 중복 방지용 Set

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new BOMInputStream(csvInputStream), StandardCharsets.UTF_8))) {

            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .parse(reader);

            for (CSVRecord record : records) {
                String ctprvnNm = record.get("CTPRVN_NM").trim();
                if (ctprvnNm == null || !"전라남도".equals(ctprvnNm)) continue;

                String rawTitle = record.get("POI_NM").trim();
                String cleanedTitle = cleanTitle(rawTitle);

                if (cleanedTitle.isEmpty()) continue;
                if (spotNameSet.contains(cleanedTitle)) continue;  // 중복 방지

                MediaSpotDTO dto = MediaSpotDTO.builder()
                        .spotId(record.get("ID").trim())
                        .spotNm(cleanedTitle)
                        .spotArea(record.get("SIGNGU_NM").trim())
                        .spotLegalDong(record.get("LEGALDONG_NM").trim())
                        .spotRi(record.get("LI_NM").trim())
                        .spotBunji(record.get("LNBR_NO").trim())
                        .spotRoadAddr(record.get("RDNMADR_NM").trim())
                        .spotLon(record.get("LC_LO").trim())
                        .spotLat(record.get("LC_LA").trim())
                        .build();

                list.add(dto);
                spotNameSet.add(cleanedTitle);  // 등록
            }

            log.info("✅ MediaSpot CSV 파싱 완료, 전라남도 중복 제거 후 총 {}개", list.size());

        } catch (Exception e) {
            log.error("❌ MediaSpot CSV 파싱 중 오류 발생", e);
        }

        return list;
    }

    /**
     * 촬영지 제목 클린업
     * - 앞에 "영화" 제거
     * - 뒤에 "촬영지", "촬영장", "영화", "세트장" 제거
     * - "나주영상테마파크" 제거
     */
    private static String cleanTitle(String title) {
        String result = title;

        // 앞에 "영화" 제거
        if (result.startsWith("영화")) {
            result = result.substring(2).trim();
        }

        // 뒤에 특정 단어 제거
        String[] suffixes = {"촬영지", "촬영장", "영화", "세트장"};
        for (String suffix : suffixes) {
            if (result.endsWith(suffix)) {
                result = result.substring(0, result.length() - suffix.length()).trim();
            }
        }

        // "나주영상테마파크" 제거 (앞이나 중간에 있으면)
        result = result.replace("나주영상테마파크", "").trim();

        return result;
    }
}