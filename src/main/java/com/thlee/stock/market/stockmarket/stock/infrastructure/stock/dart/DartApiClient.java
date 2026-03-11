package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart;

import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.config.DartProperties;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto.*;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.exception.DartApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class DartApiClient {

    private final RestClient restClient;
    private final DartProperties properties;

    public DartApiClient(@Qualifier("dartRestClient") RestClient restClient, DartProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    /**
     * 단일회사 재무계정 조회
     */
    public DartApiResponse<DartSinglAcntItem> fetchSingleAccount(
            String corpCode, String bsnsYear, String reprtCode) {
        return callReportApi("/fnlttSinglAcnt.json", corpCode, bsnsYear, reprtCode,
                new ParameterizedTypeReference<>() {});
    }

    /**
     * 다중회사 재무계정 조회
     * @param corpCodes 고유번호 목록 (쉼표 구분, 최대 100건)
     */
    public DartApiResponse<DartMultiAcntItem> fetchMultiAccount(
            String corpCodes, String bsnsYear, String reprtCode) {
        return callReportApi("/fnlttMultiAcnt.json", corpCodes, bsnsYear, reprtCode,
                new ParameterizedTypeReference<>() {});
    }

    /**
     * 단일회사 재무지표 조회
     * @param idxClCode 지표분류코드 (M210000:수익성, M220000:안정성, M230000:성장성, M240000:활동성)
     */
    public DartApiResponse<DartSinglIndxItem> fetchSingleFinancialIndex(
            String corpCode, String bsnsYear, String reprtCode, String idxClCode) {
        String uri = buildBaseUri("/fnlttSinglIndx.json")
                .queryParam("corp_code", corpCode)
                .queryParam("bsns_year", bsnsYear)
                .queryParam("reprt_code", reprtCode)
                .queryParam("idx_cl_code", idxClCode)
                .toUriString();

        return callApi(uri, new ParameterizedTypeReference<>() {});
    }

    /**
     * 다중회사 재무지표 조회
     * @param corpCodes 고유번호 목록 (쉼표 구분)
     * @param idxClCode 지표분류코드 (M210000:수익성, M220000:안정성, M230000:성장성, M240000:활동성)
     */
    public DartApiResponse<DartCmpnyIndxItem> fetchMultiFinancialIndex(
            String corpCodes, String bsnsYear, String reprtCode, String idxClCode) {
        String uri = buildBaseUri("/fnlttCmpnyIndx.json")
                .queryParam("corp_code", corpCodes)
                .queryParam("bsns_year", bsnsYear)
                .queryParam("reprt_code", reprtCode)
                .queryParam("idx_cl_code", idxClCode)
                .toUriString();

        return callApi(uri, new ParameterizedTypeReference<>() {});
    }

    /**
     * 단일회사 전체 재무제표 조회
     * @param fsDiv 개별/연결구분 (OFS: 재무제표, CFS: 연결재무제표)
     */
    public DartApiResponse<DartSinglAcntAllItem> fetchSingleFullFinancial(
            String corpCode, String bsnsYear, String reprtCode, String fsDiv) {
        String uri = buildBaseUri("/fnlttSinglAcntAll.json")
                .queryParam("corp_code", corpCode)
                .queryParam("bsns_year", bsnsYear)
                .queryParam("reprt_code", reprtCode)
                .queryParam("fs_div", fsDiv)
                .toUriString();

        return callApi(uri, new ParameterizedTypeReference<>() {});
    }

    /**
     * 주식의 총수 현황 조회
     */
    public DartApiResponse<DartStockTotqyItem> fetchStockTotalQuantity(
            String corpCode, String bsnsYear, String reprtCode) {
        return callReportApi("/stockTotqySttus.json", corpCode, bsnsYear, reprtCode,
                new ParameterizedTypeReference<>() {});
    }

    /**
     * 배당에 관한 사항 조회
     */
    public DartApiResponse<DartAlotMatterItem> fetchDividendInfo(
            String corpCode, String bsnsYear, String reprtCode) {
        return callReportApi("/alotMatter.json", corpCode, bsnsYear, reprtCode,
                new ParameterizedTypeReference<>() {});
    }

    /**
     * 소송 등의 제기 조회
     * @param bgnDe 검색시작 접수일자 (YYYYMMDD)
     * @param endDe 검색종료 접수일자 (YYYYMMDD)
     */
    public DartApiResponse<DartLawsuitItem> fetchLawsuits(
            String corpCode, String bgnDe, String endDe) {
        String uri = buildBaseUri("/lwstLg.json")
                .queryParam("corp_code", corpCode)
                .queryParam("bgn_de", bgnDe)
                .queryParam("end_de", endDe)
                .toUriString();

        return callApi(uri, new ParameterizedTypeReference<>() {});
    }

    /**
     * 사모자금 사용내역 조회
     */
    public DartApiResponse<DartPrivateFundItem> fetchPrivateFundUsage(
            String corpCode, String bsnsYear, String reprtCode) {
        return callReportApi("/prvsrpCptalUseDtls.json", corpCode, bsnsYear, reprtCode,
                new ParameterizedTypeReference<>() {});
    }

    /**
     * 공모자금 사용내역 조회
     */
    public DartApiResponse<DartPublicFundItem> fetchPublicFundUsage(
            String corpCode, String bsnsYear, String reprtCode) {
        return callReportApi("/pssrpCptalUseDtls.json", corpCode, bsnsYear, reprtCode,
                new ParameterizedTypeReference<>() {});
    }

    /**
     * DART 고유번호 전체 목록 다운로드 및 파싱
     * ZIP(CORPCODE.xml) → List<DartCorpCode>
     */
    public List<DartCorpCode> downloadCorpCodes() {
        String uri = buildBaseUri("/corpCode.xml").toUriString();

        try {
            byte[] zipBytes = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(byte[].class);

            if (zipBytes == null || zipBytes.length == 0) {
                throw new DartApiException("DART CORPCODE.xml 다운로드 실패: 빈 응답");
            }

            return parseCorpCodeZip(zipBytes);
        } catch (DartApiException e) {
            throw e;
        } catch (Exception e) {
            throw new DartApiException("DART CORPCODE.xml 다운로드 실패: " + e.getMessage(), e);
        }
    }

    // === 내부 헬퍼 메서드 ===

    /**
     * 정기보고서 API 공통 호출 (corp_code + bsns_year + reprt_code 파라미터)
     */
    private <T> DartApiResponse<T> callReportApi(
            String apiPath, String corpCode, String bsnsYear, String reprtCode,
            ParameterizedTypeReference<DartApiResponse<T>> typeRef) {
        String uri = buildBaseUri(apiPath)
                .queryParam("corp_code", corpCode)
                .queryParam("bsns_year", bsnsYear)
                .queryParam("reprt_code", reprtCode)
                .toUriString();

        return callApi(uri, typeRef);
    }

    /**
     * API 호출 공통 처리
     */
    private <T> DartApiResponse<T> callApi(
            String uri, ParameterizedTypeReference<DartApiResponse<T>> typeRef) {
        try {
            DartApiResponse<T> response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(typeRef);

            if (response != null && !response.isSuccess()) {
                throw new DartApiException(
                        "DART API 오류 [" + response.getStatus() + "]: " + response.getMessage());
            }

            return response;
        } catch (RestClientException e) {
            throw new DartApiException("DART API 호출 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 공통 URI 빌더 (base URL + 인증키 포함)
     */
    private UriComponentsBuilder buildBaseUri(String apiPath) {
        return UriComponentsBuilder
                .fromUriString(properties.getBaseUrl() + apiPath)
                .queryParam("crtfc_key", properties.getCrtfcKey());
    }

    private List<DartCorpCode> parseCorpCodeZip(byte[] zipBytes) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry = zis.getNextEntry();
            if (entry == null) {
                throw new DartApiException("DART CORPCODE.xml ZIP에 파일이 없음");
            }

            byte[] xmlBytes = zis.readAllBytes();
            return parseCorpCodeXml(new ByteArrayInputStream(xmlBytes));
        } catch (DartApiException e) {
            throw e;
        } catch (Exception e) {
            throw new DartApiException("DART CORPCODE.xml 파싱 실패: " + e.getMessage(), e);
        }
    }

    /**
     * CORPCODE.xml 구조:
     * <result>
     *   <list>
     *     <corp_code>00126380</corp_code>
     *     <corp_name>삼성전자</corp_name>
     *     <stock_code>005930</stock_code>
     *     <modify_date>20240101</modify_date>
     *   </list>
     * </result>
     */
    private List<DartCorpCode> parseCorpCodeXml(InputStream is) throws Exception {
        List<DartCorpCode> result = new ArrayList<>();
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        XMLStreamReader reader = factory.createXMLStreamReader(is);

        String corpCode = null, corpName = null, stockCode = null, modifyDate = null;
        String currentElement = null;

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                currentElement = reader.getLocalName();
            } else if (event == XMLStreamConstants.CHARACTERS && currentElement != null) {
                String text = reader.getText().trim();
                switch (currentElement) {
                    case "corp_code" -> corpCode = text;
                    case "corp_name" -> corpName = text;
                    case "stock_code" -> stockCode = text;
                    case "modify_date" -> modifyDate = text;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("list".equals(reader.getLocalName())) {
                    result.add(new DartCorpCode(corpCode, corpName, stockCode, modifyDate));
                    corpCode = null;
                    corpName = null;
                    stockCode = null;
                    modifyDate = null;
                }
                currentElement = null;
            }
        }

        reader.close();
        return result;
    }
}