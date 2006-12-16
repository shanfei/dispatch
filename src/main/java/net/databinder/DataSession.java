/*
 * Databinder: a simple bridge from Wicket to Hibernate
 * Copyright (C) 2006  Nathan Hamblen nathan@technically.us
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.databinder;

import net.databinder.conv.DataConversationRequestCycle;
import wicket.IRequestCycleFactory;
import wicket.Request;
import wicket.RequestCycle;
import wicket.Response;
import wicket.Session;
import wicket.protocol.http.WebApplication;
import wicket.protocol.http.WebRequest;
import wicket.protocol.http.WebResponse;
import wicket.protocol.http.WebSession;

/**
 * WebSession subclass whose request cycle factory instantiates DataRequestCycle
 * objects.
 *
 * @author Nathan Hamblen
 * @author Timothy Bennett
 */
public class DataSession extends WebSession {
	private boolean supportConversationSession = false;

	public DataSession(WebApplication application) {
		super(application);
	}

	public DataSession(WebApplication application, boolean supportConversationSession) {
		super(application);
		this.supportConversationSession = supportConversationSession;
	}

	/**
	 * Factory for DataRequestCycle objects.
	 */
	@Override
	public IRequestCycleFactory getRequestCycleFactory() {
		return new IRequestCycleFactory() {
			public RequestCycle newRequestCycle(Session session, Request request, Response response) {
				if (supportConversationSession)
				    return new DataConversationRequestCycle((WebSession)session, (WebRequest)request, (WebResponse)response);
				else
					return new DataRequestCycle((WebSession)session, (WebRequest)request, (WebResponse)response);
			};
		};
	}
}