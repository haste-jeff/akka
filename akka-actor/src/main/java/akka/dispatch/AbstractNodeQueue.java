/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.dispatch;

import akka.util.Unsafe;
import java.util.concurrent.atomic.AtomicReference;

// Extends AtomicReference for the "head" slot (which is the one that is appended to) since Unsafe does not expose XCHG operation intrinsically
public abstract class AbstractNodeQueue<T> extends AtomicReference<AbstractNodeQueue.Node<T>> {
    private volatile Node<T> _tailDoNotCallMeDirectly;

    protected AbstractNodeQueue() {
       final Node<T> n = new Node<T>();
       _tailDoNotCallMeDirectly = n;
       set(n);
    }

    @SuppressWarnings("unchecked")
    public final Node<T> peek() {
        return ((Node<T>)Unsafe.instance.getObjectVolatile(this, tailOffset)).next();
    }

    public final void add(final T value) {
        final Node<T> n = new Node<T>(value);
        getAndSet(n).setNext(n);
    }
    
    public final T poll() {
        final Node<T> n = peek();
        if (n == null) return null;
        Unsafe.instance.putOrderedObject(this, tailOffset, n.next());
        return n.value;
    }

    private final static long tailOffset;

    static {
        try {
          tailOffset = Unsafe.instance.objectFieldOffset(AbstractNodeQueue.class.getDeclaredField("_tailDoNotCallMeDirectly"));
        } catch(Throwable t){
            throw new ExceptionInInitializerError(t);
        }
    }

    public static class Node<T> {
        final T value;
        private volatile Node<T> _nextDoNotCallMeDirectly;

        Node() {
            this(null);
        }

        Node(final T value) {
            this.value = value;
        }

        @SuppressWarnings("unchecked")
        public final Node<T> next() {
            return (Node<T>)Unsafe.instance.getObjectVolatile(this, nextOffset);
        }

        protected final void setNext(final Node<T> newNext) {
            Unsafe.instance.putOrderedObject(this, nextOffset, newNext);
        }
        
        private final static long nextOffset;
        
        static {
            try {
                nextOffset = Unsafe.instance.objectFieldOffset(Node.class.getDeclaredField("_nextDoNotCallMeDirectly"));
            } catch(Throwable t){
                throw new ExceptionInInitializerError(t);
            } 
        }
    } 
}
