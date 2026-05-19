import React, { useCallback } from 'react';
import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Image from '@tiptap/extension-image';
import Placeholder from '@tiptap/extension-placeholder';
import { FaBold, FaItalic, FaListUl, FaListOl, FaQuoteRight, FaImage, FaUndo, FaRedo } from 'react-icons/fa';

interface ReviewEditorProps {
  initialValue?: string;
  onChange?: (content: string) => void;
  height?: string;
}

/**
 * TipTap 기반 리치 텍스트 에디터 - Neon Night 테마
 * 다크 배경 + 네온 툴바 + 글로우 포커스
 */
const ReviewEditor: React.FC<ReviewEditorProps> = ({
  initialValue = '',
  onChange,
  height = '500px',
}) => {
  const editor = useEditor({
    extensions: [
      StarterKit,
      Image.configure({
        inline: true,
        allowBase64: true,
      }),
      Placeholder.configure({
        placeholder: '공연 후기를 작성해주세요...\n\n이미지 버튼을 클릭하여 사진을 추가할 수 있습니다.',
      }),
    ],
    content: initialValue,
    onUpdate: ({ editor }) => {
      const html = editor.getHTML();
      onChange?.(html);
    },
    editorProps: {
      attributes: {
        class: 'prose prose-invert prose-sm sm:prose lg:prose-lg focus:outline-none max-w-none p-6 prose-headings:text-text-primary prose-p:text-text-secondary prose-a:text-primary prose-strong:text-text-primary prose-blockquote:border-primary/50 prose-blockquote:text-text-secondary',
      },
    },
  });

  // 파일에서 이미지 업로드 (Base64)
  const uploadImage = useCallback(() => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.onchange = (e) => {
      const file = (e.target as HTMLInputElement).files?.[0];
      if (file && editor) {
        const reader = new FileReader();
        reader.onload = (event) => {
          const src = event.target?.result as string;
          editor.chain().focus().setImage({ src }).run();
        };
        reader.readAsDataURL(file);
      }
    };
    input.click();
  }, [editor]);

  if (!editor) {
    return null;
  }

  return (
    <div className="review-editor rounded-xl overflow-hidden border border-border bg-bg-surface">
      {/* 에디터 툴바 */}
      <div className="bg-bg-elevated border-b border-border p-3 flex flex-wrap gap-1">
        {/* 텍스트 포맷 */}
        <button
          type="button"
          onClick={() => editor.chain().focus().toggleBold().run()}
          className={`p-2.5 rounded-lg transition-all duration-200 ${
            editor.isActive('bold')
              ? 'bg-primary/20 text-primary border border-primary/30'
              : 'text-text-secondary hover:bg-bg-card hover:text-text-primary border border-transparent'
          }`}
          title="굵게"
        >
          <FaBold className="w-4 h-4" />
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().toggleItalic().run()}
          className={`p-2.5 rounded-lg transition-all duration-200 ${
            editor.isActive('italic')
              ? 'bg-primary/20 text-primary border border-primary/30'
              : 'text-text-secondary hover:bg-bg-card hover:text-text-primary border border-transparent'
          }`}
          title="기울임"
        >
          <FaItalic className="w-4 h-4" />
        </button>

        <div className="w-px bg-border mx-2" />

        {/* 리스트 */}
        <button
          type="button"
          onClick={() => editor.chain().focus().toggleBulletList().run()}
          className={`p-2.5 rounded-lg transition-all duration-200 ${
            editor.isActive('bulletList')
              ? 'bg-primary/20 text-primary border border-primary/30'
              : 'text-text-secondary hover:bg-bg-card hover:text-text-primary border border-transparent'
          }`}
          title="글머리 기호"
        >
          <FaListUl className="w-4 h-4" />
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().toggleOrderedList().run()}
          className={`p-2.5 rounded-lg transition-all duration-200 ${
            editor.isActive('orderedList')
              ? 'bg-primary/20 text-primary border border-primary/30'
              : 'text-text-secondary hover:bg-bg-card hover:text-text-primary border border-transparent'
          }`}
          title="번호 매기기"
        >
          <FaListOl className="w-4 h-4" />
        </button>

        <div className="w-px bg-border mx-2" />

        {/* 인용구 */}
        <button
          type="button"
          onClick={() => editor.chain().focus().toggleBlockquote().run()}
          className={`p-2.5 rounded-lg transition-all duration-200 ${
            editor.isActive('blockquote')
              ? 'bg-primary/20 text-primary border border-primary/30'
              : 'text-text-secondary hover:bg-bg-card hover:text-text-primary border border-transparent'
          }`}
          title="인용구"
        >
          <FaQuoteRight className="w-4 h-4" />
        </button>

        <div className="w-px bg-border mx-2" />

        {/* 이미지 */}
        <button
          type="button"
          onClick={uploadImage}
          className="p-2.5 rounded-lg bg-gradient-to-r from-primary to-accent text-white hover:shadow-lg hover:shadow-primary/20 transition-all duration-200"
          title="이미지 업로드"
        >
          <FaImage className="w-4 h-4" />
        </button>

        <div className="w-px bg-border mx-2" />

        {/* 실행 취소/다시 실행 */}
        <button
          type="button"
          onClick={() => editor.chain().focus().undo().run()}
          disabled={!editor.can().undo()}
          className="p-2.5 rounded-lg text-text-secondary hover:bg-bg-card hover:text-text-primary transition-all duration-200 disabled:opacity-30 disabled:cursor-not-allowed border border-transparent"
          title="실행 취소"
        >
          <FaUndo className="w-4 h-4" />
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().redo().run()}
          disabled={!editor.can().redo()}
          className="p-2.5 rounded-lg text-text-secondary hover:bg-bg-card hover:text-text-primary transition-all duration-200 disabled:opacity-30 disabled:cursor-not-allowed border border-transparent"
          title="다시 실행"
        >
          <FaRedo className="w-4 h-4" />
        </button>
      </div>

      {/* 에디터 본문 */}
      <div
        style={{ minHeight: height, maxHeight: height }}
        className="overflow-y-auto bg-bg-deep"
      >
        <EditorContent editor={editor} />
      </div>

      {/* 커스텀 스타일 */}
      <style>{`
        .review-editor .ProseMirror {
          min-height: ${height};
          color: var(--color-text-secondary);
        }
        .review-editor .ProseMirror:focus {
          outline: none;
        }
        .review-editor .ProseMirror p.is-editor-empty:first-child::before {
          color: var(--color-text-muted);
          content: attr(data-placeholder);
          float: left;
          height: 0;
          pointer-events: none;
        }
        .review-editor .ProseMirror img {
          border-radius: 0.75rem;
          border: 1px solid var(--color-border);
          max-width: 100%;
        }
        .review-editor .ProseMirror blockquote {
          border-left: 3px solid var(--color-primary);
          padding-left: 1rem;
          margin-left: 0;
          background: rgba(129, 140, 248, 0.05);
          border-radius: 0 0.5rem 0.5rem 0;
          padding: 0.5rem 1rem;
        }
        .review-editor .ProseMirror ul,
        .review-editor .ProseMirror ol {
          padding-left: 1.5rem;
        }
        .review-editor .ProseMirror li {
          color: var(--color-text-secondary);
        }
        .review-editor .ProseMirror li::marker {
          color: var(--color-primary);
        }
        .review-editor .ProseMirror code {
          background: var(--color-bg-surface);
          padding: 0.2rem 0.4rem;
          border-radius: 0.25rem;
          color: var(--color-accent);
          font-size: 0.9em;
        }
        .review-editor .ProseMirror pre {
          background: var(--color-bg-surface);
          border: 1px solid var(--color-border);
          border-radius: 0.5rem;
          padding: 1rem;
          overflow-x: auto;
        }
        .review-editor .ProseMirror pre code {
          background: transparent;
          padding: 0;
        }
        .review-editor .ProseMirror h1,
        .review-editor .ProseMirror h2,
        .review-editor .ProseMirror h3 {
          color: var(--color-text-primary);
        }
        .review-editor .ProseMirror a {
          color: var(--color-primary);
          text-decoration: underline;
        }
        .review-editor .ProseMirror a:hover {
          color: var(--color-primary-light);
        }
      `}</style>
    </div>
  );
};

export default ReviewEditor;
