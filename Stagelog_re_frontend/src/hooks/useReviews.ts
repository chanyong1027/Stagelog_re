import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
// import { toast } from 'react-hot-toast';
import { reviewAPI } from '../api/review.api';
import { ReviewCreateRequest, ReviewUpdateRequest } from '../types/review.types';
import { ROUTES } from '../utils/constants';

/**
 * 내 리뷰 목록 조회 Hook
 */
export const useMyReviews = () => {
  return useQuery({
    queryKey: ['reviews', 'my'],
    queryFn: () => reviewAPI.getMyReviews().then(res => res.data),
  });
};

/**
 * 리뷰 상세 조회 Hook
 * @param id - 리뷰 ID
 */
export const useReviewDetail = (id: number) => {
  return useQuery({
    queryKey: ['review', id],
    queryFn: () => reviewAPI.getReviewById(id).then(res => res.data),
    enabled: !!id, // id가 있을 때만 쿼리 실행
  });
};

/**
 * 리뷰 생성 Hook
 */
export const useCreateReview = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: ReviewCreateRequest) => reviewAPI.createReview(data),
    onSuccess: (response) => {
      const reviewId = response.data;
      // toast.success(SUCCESS_MESSAGES.REVIEW.CREATE_SUCCESS);
      // 리뷰 목록 캐시 무효화 (새로고침)
      queryClient.invalidateQueries({ queryKey: ['reviews'] });
      // 생성된 리뷰 상세 페이지로 이동
      navigate(ROUTES.REVIEW_DETAIL(reviewId));
    },
    onError: () => {
      // toast.error('리뷰 작성에 실패했습니다.');
    },
  });
};

/**
 * 리뷰 수정 Hook
 */
export const useUpdateReview = (reviewId: number) => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: ReviewUpdateRequest) => reviewAPI.updateReview(reviewId, data),
    onSuccess: () => {
      // toast.success(SUCCESS_MESSAGES.REVIEW.UPDATE_SUCCESS);
      // 리뷰 목록 및 상세 캐시 무효화 (새로고침)
      queryClient.invalidateQueries({ queryKey: ['reviews'] });
      queryClient.invalidateQueries({ queryKey: ['review', reviewId] });
      // 수정된 리뷰 상세 페이지로 이동
      navigate(ROUTES.REVIEW_DETAIL(reviewId));
    },
    onError: () => {
      // toast.error('리뷰 수정에 실패했습니다.');
    },
  });
};

/**
 * 리뷰 삭제 Hook
 */
export const useDeleteReview = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => reviewAPI.deleteReview(id),
    onSuccess: () => {
      // toast.success(SUCCESS_MESSAGES.REVIEW.DELETE_SUCCESS);
      // 리뷰 목록 캐시 무효화 (새로고침)
      queryClient.invalidateQueries({ queryKey: ['reviews'] });
      // 리뷰 목록 페이지로 이동
      navigate(ROUTES.REVIEWS);
    },
    onError: () => {
      // toast.error('리뷰 삭제에 실패했습니다.');
    },
  });
};
