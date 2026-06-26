import React, { useState } from 'react';
import { TrackRequest } from '../../types/review.types';
import { spotifyAPI, SpotifyTrack } from '../../api/spotify.api';

interface SpotifyPlaylistBuilderProps {
  tracks: TrackRequest[];
  onTracksChange: (tracks: TrackRequest[]) => void;
  playlistTitle: string;
  onPlaylistTitleChange: (title: string) => void;
}

/**
 * Spotify 스타일 플레이리스트 빌더
 * - 트랙 검색 (Spotify Web API)
 * - 플레이리스트 구성
 * - Spotify UI 스타일
 */
const SpotifyPlaylistBuilder: React.FC<SpotifyPlaylistBuilderProps> = ({
  tracks,
  onTracksChange,
  playlistTitle,
  onPlaylistTitleChange,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<SpotifyTrack[]>([]);
  const [isSearching, setIsSearching] = useState(false);

  const handleSearch = async (e?: React.FormEvent | React.KeyboardEvent) => {
    e?.preventDefault();
    if (!searchQuery.trim()) return;

    setIsSearching(true);

    try {
      const response = await spotifyAPI.searchTracks(searchQuery, 30);
      setSearchResults(response.data.tracks.items);
    } catch (error) {
      console.error('Spotify 검색 실패:', error);
      alert('곡 검색에 실패했습니다. 다시 시도해주세요.');
      setSearchResults([]);
    } finally {
      setIsSearching(false);
    }
  };

  const handleAddTrack = (track: SpotifyTrack) => {
    const newTrack: TrackRequest = {
      spotifyId: track.id,
      title: track.name,
      artistName: track.artists.map(a => a.name).join(', '),
      albumImageUrl: track.album.images[0]?.url || '',
    };

    // 중복 체크
    if (tracks.some(t => t.spotifyId === newTrack.spotifyId)) {
      alert('이미 추가된 곡입니다.');
      return;
    }

    onTracksChange([...tracks, newTrack]);
    setSearchQuery('');
    setSearchResults([]);
  };

  const handleRemoveTrack = (index: number) => {
    onTracksChange(tracks.filter((_, i) => i !== index));
  };

  return (
    <div className="bg-gradient-to-b from-gray-900 to-black rounded-2xl overflow-hidden">
      {/* 플레이리스트 헤더 */}
      <div className="p-8 bg-gradient-to-b from-gray-800 to-gray-900">
        <div className="flex items-end gap-6">
          {/* 플레이리스트 커버 */}
          <div className="flex-shrink-0 w-48 h-48 bg-gradient-to-br from-purple-500 via-pink-500 to-orange-500 rounded-lg shadow-2xl flex items-center justify-center">
            <svg className="w-24 h-24 text-white opacity-80" fill="currentColor" viewBox="0 0 20 20">
              <path d="M18 3a1 1 0 00-1.196-.98l-10 2A1 1 0 006 5v9.114A4.369 4.369 0 005 14c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V7.82l8-1.6v5.894A4.37 4.37 0 0015 12c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V3z" />
            </svg>
          </div>

          {/* 플레이리스트 정보 */}
          <div className="flex-1">
            <p className="text-xs font-semibold text-white uppercase tracking-wider mb-2">
              플레이리스트
            </p>
            <input
              type="text"
              value={playlistTitle}
              onChange={(e) => onPlaylistTitleChange(e.target.value)}
              className="text-5xl font-bold text-white mb-4 bg-transparent border-none outline-none focus:outline-none hover:bg-white/10 px-2 py-1 rounded transition-colors w-full"
              placeholder="플레이리스트 제목"
            />
            <p className="text-sm text-gray-300">
              {tracks.length}곡 · 공연 후기
            </p>
          </div>
        </div>
      </div>

      {/* 검색 바 */}
      <div className="p-6 border-b border-gray-800">
        <div className="relative">
          <svg className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault();
                handleSearch(e as unknown as React.FormEvent);
              }
            }}
            placeholder="곡, 아티스트 검색"
            className="w-full pl-12 pr-4 py-3 bg-gray-800 text-white rounded-full border border-gray-700 focus:outline-none focus:border-white placeholder-gray-400"
          />
        </div>

        {/* 검색 결과 */}
        {isSearching && (
          <div className="mt-4 p-4 text-center">
            <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-white"></div>
            <p className="text-sm text-gray-400 mt-2">검색 중...</p>
          </div>
        )}

        {!isSearching && searchResults.length > 0 && (
          <div className="mt-4 bg-gray-800 rounded-lg overflow-hidden max-h-[320px] overflow-y-auto scrollbar-thin scrollbar-thumb-gray-600 scrollbar-track-gray-800">
            {searchResults.map((track) => (
              <div
                key={track.id}
                className="flex items-center gap-4 p-3 hover:bg-gray-700 transition-colors group"
              >
                <img
                  src={track.album.images[0]?.url}
                  alt={track.album.name}
                  className="w-12 h-12 rounded"
                />
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-white truncate">{track.name}</p>
                  <p className="text-sm text-gray-400 truncate">
                    {track.artists.map(a => a.name).join(', ')}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => handleAddTrack(track)}
                  className="opacity-0 group-hover:opacity-100 px-4 py-2 bg-white text-black rounded-full font-semibold hover:scale-105 transition-all text-sm"
                >
                  추가
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 트랙 리스트 */}
      <div className="px-6 py-4">
        {tracks.length > 0 ? (
          <div className="space-y-1">
            {tracks.map((track, index) => (
              <div
                key={index}
                className="flex items-center gap-4 p-2 rounded-md hover:bg-gray-800 transition-colors group"
              >
                {/* 트랙 번호 */}
                <div className="w-8 text-center">
                  <span className="text-gray-400 text-sm font-medium group-hover:hidden">
                    {index + 1}
                  </span>
                  <button
                    type="button"
                    onClick={() => handleRemoveTrack(index)}
                    className="hidden group-hover:block text-red-400 hover:text-red-300"
                  >
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>

                {/* 앨범 아트 */}
                {track.albumImageUrl ? (
                  <img
                    src={track.albumImageUrl}
                    alt={track.title}
                    className="w-10 h-10 rounded"
                    onError={(e) => {
                      e.currentTarget.style.display = 'none';
                    }}
                  />
                ) : (
                  <div className="w-10 h-10 bg-gray-700 rounded flex items-center justify-center">
                    <svg className="w-5 h-5 text-gray-500" fill="currentColor" viewBox="0 0 20 20">
                      <path d="M18 3a1 1 0 00-1.196-.98l-10 2A1 1 0 006 5v9.114A4.369 4.369 0 005 14c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V7.82l8-1.6v5.894A4.37 4.37 0 0015 12c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V3z" />
                    </svg>
                  </div>
                )}

                {/* 트랙 정보 */}
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-white truncate">{track.title}</p>
                  {track.artistName && (
                    <p className="text-sm text-gray-400 truncate">{track.artistName}</p>
                  )}
                </div>

                {/* Spotify 링크 */}
                {track.spotifyId && (
                  <a
                    href={`https://open.spotify.com/track/${track.spotifyId}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="opacity-0 group-hover:opacity-100 text-gray-400 hover:text-white transition-all"
                    title="Spotify에서 열기"
                  >
                    <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                      <path d="M12 0C5.4 0 0 5.4 0 12s5.4 12 12 12 12-5.4 12-12S18.66 0 12 0zm5.521 17.34c-.24.359-.66.48-1.021.24-2.82-1.74-6.36-2.101-10.561-1.141-.418.122-.779-.179-.899-.539-.12-.421.18-.78.54-.9 4.56-1.021 8.52-.6 11.64 1.32.42.18.479.659.301 1.02zm1.44-3.3c-.301.42-.841.6-1.262.3-3.239-1.98-8.159-2.58-11.939-1.38-.479.12-1.02-.12-1.14-.6-.12-.48.12-1.021.6-1.141C9.6 9.9 15 10.561 18.72 12.84c.361.181.54.78.241 1.2zm.12-3.36C15.24 8.4 8.82 8.16 5.16 9.301c-.6.179-1.2-.181-1.38-.721-.18-.601.18-1.2.72-1.381 4.26-1.26 11.28-1.02 15.721 1.621.539.3.719 1.02.419 1.56-.299.421-1.02.599-1.559.3z" />
                    </svg>
                  </a>
                )}
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-12">
            <svg className="w-16 h-16 mx-auto mb-4 text-gray-600" fill="currentColor" viewBox="0 0 20 20">
              <path d="M18 3a1 1 0 00-1.196-.98l-10 2A1 1 0 006 5v9.114A4.369 4.369 0 005 14c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V7.82l8-1.6v5.894A4.37 4.37 0 0015 12c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V3z" />
            </svg>
            <p className="text-gray-400 text-sm">아직 추가된 곡이 없습니다</p>
            <p className="text-gray-500 text-xs mt-1">검색하여 곡을 추가해보세요</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default SpotifyPlaylistBuilder;
