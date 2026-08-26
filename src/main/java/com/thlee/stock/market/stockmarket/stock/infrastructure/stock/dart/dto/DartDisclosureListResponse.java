package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 공시검색(list.json) 응답 봉투.
 * 기존 {@link DartApiResponse}와 달리 페이징 필드(total_count 등)를 포함한다.
 */
@Getter
@NoArgsConstructor
public class DartDisclosureListResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("page_no")
    private Integer pageNo;

    @JsonProperty("page_count")
    private Integer pageCount;

    @JsonProperty("total_count")
    private Integer totalCount;

    @JsonProperty("total_page")
    private Integer totalPage;

    @JsonProperty("list")
    private List<DartDisclosureItem> list;

    public List<DartDisclosureItem> getList() {
        return list != null ? list : Collections.emptyList();
    }

    public boolean isSuccess() {
        return getStatusCode().isSuccess();
    }

    public boolean isNoData() {
        return getStatusCode().isNoData();
    }

    public DartStatusCode getStatusCode() {
        return DartStatusCode.fromCode(status);
    }

    public static DartDisclosureListResponse empty() {
        DartDisclosureListResponse response = new DartDisclosureListResponse();
        response.status = DartStatusCode.SUCCESS.getCode();
        response.message = "정상";
        response.list = Collections.emptyList();
        response.totalCount = 0;
        return response;
    }
}
