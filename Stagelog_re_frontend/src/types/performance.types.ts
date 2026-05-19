// 공연 목록 응답 (간소화된 정보)
export interface PerformanceListItem {
  id: number;
  title: string;
  postUrl: string;  // 포스터 URL
  startDate: string;
  endDate: string;
}

// 공연 상세 응답
export interface PerformanceDetail {
  id: number;
  title: string;
  postUrl: string;  // 포스터 URL
  cast: string[];  // 출연진 목록
  startDate: string;
  endDate: string;
  runtime: string;
  dtguidance: string;  // 공연 시작 시간
  place: string;  // 장소
  ticketPrice: string;
  ticketVendor: string;
  ticketUrl: string;
}

// 캘린더 공연 응답
export interface CalendarPerformance {
  id: number;
  title: string;
  startDate: string;
  endDate: string;
  postUrl?: string;
}

// 공연 필터
export interface PerformanceFilters {
  isFestival?: boolean | null;
  keyword?: string;
  page?: number;
  size?: number;
  sort?: string;
}
