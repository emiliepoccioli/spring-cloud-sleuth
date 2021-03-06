/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.instrument.zuul;

import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanInjector;
import org.springframework.cloud.sleuth.Tracer;

import com.netflix.zuul.ExecutionStatus;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.ZuulFilterResult;
import com.netflix.zuul.context.RequestContext;

/**
 * A pre request {@link ZuulFilter} that sets tracing related headers on the request
 * from the current span. We're doing so to ensure tracing propagates to the next hop.
 *
 * @author Dave Syer
 * @since 1.0.0
 */
public class TracePreZuulFilter extends ZuulFilter {

	private static final String ZUUL_COMPONENT = "zuul";

	private final Tracer tracer;
	private final SpanInjector<RequestContext> spanInjector;

	public TracePreZuulFilter(Tracer tracer, SpanInjector<RequestContext> spanInjector) {
		this.tracer = tracer;
		this.spanInjector = spanInjector;
	}

	@Override
	public boolean shouldFilter() {
		return true;
	}

	@Override
	public Object run() {
		getCurrentSpan().logEvent(Span.CLIENT_SEND);
		return null;
	}

	@Override
	public ZuulFilterResult runFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		Span span = getCurrentSpan();
		Span newSpan = this.tracer.createSpan(span.getName(), span);
		newSpan.tag(Span.SPAN_LOCAL_COMPONENT_TAG_NAME, ZUUL_COMPONENT);
		this.spanInjector.inject(newSpan, ctx);
		ZuulFilterResult result = super.runFilter();
		if (ExecutionStatus.SUCCESS != result.getStatus()) {
			this.tracer.close(newSpan);
		}
		return result;
	}

	private Span getCurrentSpan() {
		return this.tracer.getCurrentSpan();
	}


	@Override
	public String filterType() {
		return "pre";
	}

	@Override
	public int filterOrder() {
		return 0;
	}

}
