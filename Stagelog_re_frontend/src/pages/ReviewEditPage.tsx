import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useReviewDetail, useUpdateReview } from '../hooks/useReviews';
import { useAuthStore } from '../store/authStore';
import Header from '../components/layout/Header';
import Footer from '../components/layout/Footer';
import ReviewEditor from '../components/review/ReviewEditor';
import SpotifyPlaylistBuilder from '../components/review/SpotifyPlaylistBuilder';
import Input from '../components/common/Input';
import Button from '../components/common/Button';
import Loading from '../components/common/Loading';
import { ROUTES } from '../utils/constants';
import { TrackRequest } from '../types/review.types';

/**
 * 리뷰 수정 페이지 - Neon Night 테마
 * 다크 배경 + 글래스 카드 + 네온 액센트
 */
const ReviewEditPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const reviewId = Number(id);
  const nickname = useAuthStore((state) => state.nickname);

  const { data: review, isLoading: isLoadingReview } = useReviewDetail(reviewId);
  const { mutate: updateReview, isPending } = useUpdateReview(reviewId);

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [tracks, setTracks] = useState<TrackRequest[]>([]);
  const [playlistTitle, setPlaylistTitle] = useState(`${nickname || '나'}의 Playlist`);
  const [errors, setErrors] = useState<{ title?: string; content?: string }>({});

  // 리뷰 데이터 로드 후 폼 상태 초기화. 서버 데이터 → 폼 state 매핑은
  // setState-in-effect 패턴이 적합하나 lint 규칙이 이를 막으므로 블록 단위로 비활성.
  // TODO: 추후 react-hook-form + defaultValues로 리팩토링.
  /* eslint-disable react-hooks/set-state-in-effect */
  useEffect(() => {
    if (review) {
      setTitle(review.title);
      setContent(review.content);
      if (review.playlistTitle) {
        setPlaylistTitle(review.playlistTitle);
      }
      if (review.tracks && review.tracks.length > 0) {
        setTracks(review.tracks.map(track => ({
          spotifyId: track.spotifyId,
          title: track.title,
          artistName: track.artistName,
          albumImageUrl: track.albumImageUrl,
        })));
      }
    }
  }, [review]);
  /* eslint-enable react-hooks/set-state-in-effect */

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    // 유효성 검사
    const newErrors: { title?: string; content?: string } = {};
    if (!title.trim()) {
      newErrors.title = '제목을 입력해주세요.';
    }
    if (!content.trim() || content === '<p></p>') {
      newErrors.content = '내용을 입력해주세요.';
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    // 리뷰 수정
    updateReview({
      title,
      content,
      playlistTitle: tracks.length > 0 ? playlistTitle : undefined,
      tracks: tracks.length > 0 ? tracks : undefined,
    });
  };

  // 로딩 상태
  if (isLoadingReview) {
    return (
      <div className="min-h-screen bg-bg-base">
        <div className="noise-overlay" />
        <div className="fixed inset-0 bg-mesh pointer-events-none" />
        <Header />
        <div className="flex items-center justify-center min-h-[60vh]">
          <Loading />
        </div>
        <Footer />
      </div>
    );
  }

  // 리뷰 없음
  if (!review) {
    return (
      <div className="min-h-screen bg-bg-base">
        <div className="noise-overlay" />
        <div className="fixed inset-0 bg-mesh pointer-events-none" />
        <Header />
        <div className="relative max-w-4xl mx-auto px-6 py-16 text-center">
          <div className="bg-bg-card rounded-2xl border border-border p-12">
            <div className="w-20 h-20 mx-auto mb-6 rounded-full bg-bg-surface border border-border flex items-center justify-center">
              <span className="text-4xl">😢</span>
            </div>
            <h2 className="text-2xl font-bold text-text-primary mb-4">
              리뷰를 불러올 수 없습니다
            </h2>
            <p className="text-text-secondary mb-8">
              요청하신 리뷰를 찾을 수 없거나 권한이 없습니다.
            </p>
            <Button onClick={() => navigate(ROUTES.REVIEWS)} className="btn-neon">
              리뷰 목록으로
            </Button>
          </div>
        </div>
        <Footer />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-bg-base">
      {/* 노이즈 오버레이 */}
      <div className="noise-overlay" />

      {/* 배경 메쉬 그라데이션 */}
      <div className="fixed inset-0 bg-mesh pointer-events-none" />

      <Header />

      <main className="relative max-w-5xl mx-auto px-6 py-12">
        {/* 페이지 헤더 */}
        <div className="mb-10 animate-fade-in-up">
          <div className="flex items-center gap-3 mb-3">
            <span className="text-4xl">✏️</span>
            <h1 className="text-display text-text-primary">리뷰 수정</h1>
          </div>
          <p className="text-text-secondary text-lg">
            공연 후기를 수정하세요
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* 제목 입력 */}
          <div className="animate-fade-in-up stagger-1">
            <div className="relative bg-bg-card rounded-2xl border border-border p-6 overflow-hidden">
              {/* 상단 그라데이션 라인 */}
              <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-primary/50 to-transparent" />

              <Input
                label="리뷰 제목"
                type="text"
                placeholder="제목을 입력해주세요."
                value={title}
                onChange={(e) => {
                  setTitle(e.target.value);
                  if (errors.title) setErrors({ ...errors, title: undefined });
                }}
                error={errors.title}
                required
              />
            </div>
          </div>

          {/* 본문 에디터 */}
          <div className="animate-fade-in-up stagger-2">
            <div className="relative bg-bg-card rounded-2xl border border-border p-6 overflow-hidden">
              {/* 상단 그라데이션 라인 */}
              <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-accent/50 to-transparent" />

              <label className="block text-sm font-medium text-text-primary mb-3">
                리뷰 내용 <span className="text-accent">*</span>
              </label>
              <p className="text-xs text-text-muted mb-4 flex items-center gap-2">
                <span className="px-2 py-1 bg-bg-surface rounded-md border border-border">
                  📷 이미지 업로드 지원: JPG, PNG, GIF, WebP
                </span>
              </p>
              <div className="rounded-xl overflow-hidden border border-border">
                <ReviewEditor
                  initialValue={content}
                  onChange={(html) => {
                    setContent(html);
                    if (errors.content) setErrors({ ...errors, content: undefined });
                  }}
                  height="500px"
                />
              </div>
              {errors.content && (
                <p className="mt-3 text-sm text-accent flex items-center gap-2">
                  <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                  </svg>
                  {errors.content}
                </p>
              )}
            </div>
          </div>

          {/* Spotify 플레이리스트 섹션 */}
          <div className="animate-fade-in-up stagger-3">
            <div className="relative bg-bg-card rounded-2xl border border-border p-6 overflow-hidden">
              {/* 상단 그라데이션 라인 */}
              <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-secondary/50 to-transparent" />

              <div className="flex items-center gap-4 mb-6">
                <div className="w-12 h-12 bg-gradient-to-br from-secondary to-secondary-dark rounded-xl flex items-center justify-center shadow-lg shadow-secondary/20">
                  <svg className="w-6 h-6 text-white" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M18 3a1 1 0 00-1.196-.98l-10 2A1 1 0 006 5v9.114A4.369 4.369 0 005 14c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V7.82l8-1.6v5.894A4.37 4.37 0 0015 12c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V3z" />
                  </svg>
                </div>
                <div>
                  <h3 className="text-xl font-semibold text-text-primary">
                    플레이리스트 <span className="text-text-muted font-normal text-sm">(선택)</span>
                  </h3>
                  <p className="text-sm text-text-muted">
                    공연에서 들었던 곡들을 Spotify 스타일로 추가해보세요
                  </p>
                </div>
              </div>

              {/* Spotify 플레이리스트 빌더 */}
              <SpotifyPlaylistBuilder
                tracks={tracks}
                onTracksChange={setTracks}
                playlistTitle={playlistTitle}
                onPlaylistTitleChange={setPlaylistTitle}
              />
            </div>
          </div>

          {/* 액션 버튼 */}
          <div className="flex gap-4 justify-end animate-fade-in-up stagger-4">
            <Button
              type="button"
              variant="secondary"
              onClick={() => navigate(ROUTES.REVIEW_DETAIL(reviewId))}
              disabled={isPending}
              className="bg-bg-surface border-border hover:border-border-light"
            >
              취소
            </Button>
            <Button
              type="submit"
              loading={isPending}
              disabled={isPending}
              className="btn-neon"
            >
              <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
              수정 완료
            </Button>
          </div>
        </form>
      </main>

      <Footer />
    </div>
  );
};

export default ReviewEditPage;
