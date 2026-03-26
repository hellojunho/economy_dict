import { useCallback, useEffect, useRef, useState } from 'react';

type Options = {
  threadKey?: string | null;
  itemCount: number;
  loading: boolean;
};

const BOTTOM_THRESHOLD = 56;

function isNearBottom(element: HTMLDivElement) {
  const remaining = element.scrollHeight - element.scrollTop - element.clientHeight;
  return remaining <= BOTTOM_THRESHOLD;
}

export default function useTranscriptScroll({ threadKey, itemCount, loading }: Options) {
  const viewportRef = useRef<HTMLDivElement | null>(null);
  const stickToBottomRef = useRef(true);
  const previousThreadKeyRef = useRef<string | null | undefined>(threadKey);
  const [showScrollButton, setShowScrollButton] = useState(false);

  const scrollToBottom = useCallback((behavior: ScrollBehavior = 'smooth') => {
    const viewport = viewportRef.current;
    if (!viewport) {
      return;
    }

    viewport.scrollTo({
      top: viewport.scrollHeight,
      behavior
    });
    stickToBottomRef.current = true;
    setShowScrollButton(false);
  }, []);

  const handleScroll = useCallback(() => {
    const viewport = viewportRef.current;
    if (!viewport) {
      return;
    }

    const nearBottom = isNearBottom(viewport);
    stickToBottomRef.current = nearBottom;
    setShowScrollButton(!nearBottom);
  }, []);

  useEffect(() => {
    if (!threadKey) {
      setShowScrollButton(false);
      return;
    }

    if (previousThreadKeyRef.current !== threadKey) {
      previousThreadKeyRef.current = threadKey;
      requestAnimationFrame(() => {
        scrollToBottom('auto');
      });
    }
  }, [scrollToBottom, threadKey]);

  useEffect(() => {
    if (!threadKey) {
      return;
    }

    requestAnimationFrame(() => {
      const viewport = viewportRef.current;
      if (!viewport) {
        return;
      }

      if (stickToBottomRef.current || isNearBottom(viewport)) {
        scrollToBottom(itemCount <= 1 && !loading ? 'auto' : 'smooth');
      } else {
        setShowScrollButton(true);
      }
    });
  }, [itemCount, loading, scrollToBottom, threadKey]);

  return {
    viewportRef,
    handleScroll,
    scrollToBottom,
    showScrollButton
  };
}
