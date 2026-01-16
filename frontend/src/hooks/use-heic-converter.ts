'use client';

import { useState, useEffect, useRef } from 'react';

// IndexedDB 缓存管理
const DB_NAME = 'heic-cache';
const STORE_NAME = 'converted-images';
const DB_VERSION = 1;

let dbPromise: Promise<IDBDatabase> | null = null;

function openDB(): Promise<IDBDatabase> {
    if (dbPromise) return dbPromise;

    dbPromise = new Promise((resolve, reject) => {
        const request = indexedDB.open(DB_NAME, DB_VERSION);

        request.onerror = () => reject(request.error);
        request.onsuccess = () => resolve(request.result);

        request.onupgradeneeded = (event) => {
            const db = (event.target as IDBOpenDBRequest).result;
            if (!db.objectStoreNames.contains(STORE_NAME)) {
                db.createObjectStore(STORE_NAME);
            }
        };
    });

    return dbPromise;
}

async function getCachedImage(url: string): Promise<Blob | null> {
    try {
        const db = await openDB();
        const transaction = db.transaction(STORE_NAME, 'readonly');
        const store = transaction.objectStore(STORE_NAME);

        return new Promise((resolve, reject) => {
            const request = store.get(url);
            request.onsuccess = () => resolve(request.result || null);
            request.onerror = () => reject(request.error);
        });
    } catch (error) {
        console.error('Error getting cached image:', error);
        return null;
    }
}

async function setCachedImage(url: string, blob: Blob): Promise<void> {
    try {
        const db = await openDB();
        const transaction = db.transaction(STORE_NAME, 'readwrite');
        const store = transaction.objectStore(STORE_NAME);

        return new Promise((resolve, reject) => {
            const request = store.put(blob, url);
            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    } catch (error) {
        console.error('Error caching image:', error);
    }
}

/**
 * Hook to convert HEIC images to JPEG for browser display with caching
 */
export function useHeicConverter(url: string | undefined) {
    const [convertedUrl, setConvertedUrl] = useState<string | undefined>(url);
    const [isConverting, setIsConverting] = useState(false);
    const [error, setError] = useState<Error | null>(null);
    const abortControllerRef = useRef<AbortController | null>(null);

    useEffect(() => {
        if (!url) {
            setConvertedUrl(undefined);
            return;
        }

        // Check if URL points to a HEIC file
        const isHeic = /\.heic$/i.test(url) || /\.heif$/i.test(url);

        if (!isHeic) {
            setConvertedUrl(url);
            return;
        }

        // Abort previous conversion if any
        if (abortControllerRef.current) {
            abortControllerRef.current.abort();
        }
        abortControllerRef.current = new AbortController();
        const signal = abortControllerRef.current.signal;

        // Convert HEIC to JPEG with caching
        const convertHeic = async () => {
            setIsConverting(true);
            setError(null);

            try {
                // Check cache first
                const cachedBlob = await getCachedImage(url);

                if (cachedBlob && !signal.aborted) {
                    const objectUrl = URL.createObjectURL(cachedBlob);
                    setConvertedUrl(objectUrl);
                    setIsConverting(false);
                    return;
                }

                if (signal.aborted) return;

                // Dynamically import heic2any only when needed
                const heic2any = (await import('heic2any')).default;

                if (signal.aborted) return;

                // Fetch the HEIC image
                const response = await fetch(url, { signal });
                const blob = await response.blob();

                if (signal.aborted) return;

                // Convert to JPEG with lower quality for faster conversion
                const convertedBlob = await heic2any({
                    blob,
                    toType: 'image/jpeg',
                    quality: 0.7, // 降低质量从 0.9 到 0.7，加快转换速度
                }) as Blob;

                if (signal.aborted) return;

                // Cache the converted image
                await setCachedImage(url, convertedBlob);

                // Create object URL for the converted image
                const objectUrl = URL.createObjectURL(convertedBlob);
                setConvertedUrl(objectUrl);
            } catch (err: any) {
                if (err.name === 'AbortError') return;

                console.error('HEIC conversion error:', err);
                setError(err as Error);
                // Fallback to original URL if conversion fails
                setConvertedUrl(url);
            } finally {
                if (!signal.aborted) {
                    setIsConverting(false);
                }
            }
        };

        convertHeic();

        // Cleanup
        return () => {
            if (abortControllerRef.current) {
                abortControllerRef.current.abort();
            }
            if (convertedUrl && convertedUrl.startsWith('blob:')) {
                URL.revokeObjectURL(convertedUrl);
            }
        };
    }, [url]);

    return { convertedUrl, isConverting, error };
}
