/*******************************************************************************
 * Copyright (c) 2009, 2010, 2012 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *   Francois Chouinard - Added tests to check offsets
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.core.tests.request;

import junit.framework.TestCase;

import org.eclipse.linuxtools.internal.tmf.core.request.TmfCoalescedDataRequest;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.TmfEvent;
import org.eclipse.linuxtools.tmf.core.request.TmfDataRequest;
import org.eclipse.linuxtools.tmf.tests.stubs.request.TmfDataRequestStub;

/**
 * Test suite for the TmfCoalescedDataRequest class.
 */
@SuppressWarnings({"nls","javadoc"})
public class TmfCoalescedDataRequestTest extends TestCase {

	// ------------------------------------------------------------------------
	// Variables
	// ------------------------------------------------------------------------

	private TmfCoalescedDataRequest fRequest1;
	private TmfCoalescedDataRequest fRequest2;
	private TmfCoalescedDataRequest fRequest3;
	private TmfCoalescedDataRequest fRequest4;

	private TmfCoalescedDataRequest fRequest1b;
	private TmfCoalescedDataRequest fRequest1c;

	private int fRequestCount;

	// ------------------------------------------------------------------------
	// Housekeeping
	// ------------------------------------------------------------------------

	/**
	 * @param name the test name
	 */
	public TmfCoalescedDataRequestTest(final String name) {
		super(name);
	}

    @Override
	public void setUp() throws Exception {
		super.setUp();
		TmfDataRequest.reset();
		fRequest1  = new TmfCoalescedDataRequest(TmfEvent.class, 10, 100, 200);
		fRequest2  = new TmfCoalescedDataRequest(TmfEvent.class, 20, 100, 200);
		fRequest3  = new TmfCoalescedDataRequest(TmfEvent.class, 20, 200, 200);
		fRequest4  = new TmfCoalescedDataRequest(TmfEvent.class, 20, 200, 300);

		fRequest1b = new TmfCoalescedDataRequest(TmfEvent.class, 10, 100, 200);
		fRequest1c = new TmfCoalescedDataRequest(TmfEvent.class, 10, 100, 200);

		fRequestCount = fRequest1c.getRequestId() + 1;
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
	}

	private static TmfCoalescedDataRequest setupTestRequest(final boolean[] flags) {

		TmfCoalescedDataRequest request = new TmfCoalescedDataRequest(TmfEvent.class, 10, 100, 200) {
		    @Override
			public void handleCompleted() {
		    	super.handleCompleted();
		    	flags[0] = true;
		    }
		    @Override
		    public void handleSuccess() {
		    	super.handleSuccess();
		    	flags[1] = true;
		    }
		    @Override
		    public void handleFailure() {
		    	super.handleFailure();
		    	flags[2] = true;
		    }
		    @Override
		    public void handleCancel() {
		    	super.handleCancel();
		    	flags[3] = true;
		    }
		};
		return request;
	}

	// ------------------------------------------------------------------------
	// Constructors
	// ------------------------------------------------------------------------

	public void testTmfCoalescedDataRequest() {
		TmfCoalescedDataRequest request = new TmfCoalescedDataRequest(TmfEvent.class);

        assertEquals("getRequestId", fRequestCount++, request.getRequestId());
        assertEquals("getDataType",  ITmfEvent.class, request.getDataType());

        assertEquals("getIndex", 0, request.getIndex());
        assertEquals("getNbRequestedEvents", TmfDataRequest.ALL_DATA, request.getNbRequested());

        assertFalse("isCompleted", request.isCompleted());
        assertFalse("isFailed", request.isFailed());
        assertFalse("isCancelled", request.isCancelled());

        assertEquals("getNbRead", 0, request.getNbRead());
	}

	public void testTmfCoalescedDataRequestIndex() {
		TmfCoalescedDataRequest request = new TmfCoalescedDataRequest(TmfEvent.class, 10);

        assertEquals("getRequestId", fRequestCount++, request.getRequestId());
        assertEquals("getDataType",  ITmfEvent.class, request.getDataType());

        assertEquals("getIndex", 10, request.getIndex());
        assertEquals("getNbRequestedEvents", TmfDataRequest.ALL_DATA, request.getNbRequested());

        assertFalse("isCompleted", request.isCompleted());
        assertFalse("isFailed", request.isFailed());
        assertFalse("isCancelled", request.isCancelled());

        assertEquals("getNbRead", 0, request.getNbRead());
	}

	public void testTmfCoalescedDataRequestIndexNbRequested() {
		TmfCoalescedDataRequest request = new TmfCoalescedDataRequest(TmfEvent.class, 10, 100);

        assertEquals("getRequestId", fRequestCount++, request.getRequestId());
        assertEquals("getDataType",  ITmfEvent.class, request.getDataType());

        assertEquals("getIndex", 10, request.getIndex());
        assertEquals("getNbRequestedEvents", 100, request.getNbRequested());

        assertFalse("isCompleted", request.isCompleted());
        assertFalse("isFailed", request.isFailed());
        assertFalse("isCancelled", request.isCancelled());

        assertEquals("getNbRead", 0, request.getNbRead());
	}

	public void testTmfCoalescedDataRequestIndexNbEventsBlocksize() {
		TmfCoalescedDataRequest request = new TmfCoalescedDataRequest(TmfEvent.class, 10, 100, 200);

        assertEquals("getRequestId", fRequestCount++, request.getRequestId());
        assertEquals("getDataType",  ITmfEvent.class, request.getDataType());

        assertEquals("getIndex", 10, request.getIndex());
        assertEquals("getNbRequestedEvents", 100, request.getNbRequested());

        assertFalse("isCompleted", request.isCompleted());
        assertFalse("isFailed", request.isFailed());
        assertFalse("isCancelled", request.isCancelled());

        assertEquals("getNbRead", 0, request.getNbRead());
	}

	// ------------------------------------------------------------------------
	// equals
	// ------------------------------------------------------------------------

	public void testEqualsReflexivity() {
        assertTrue("equals", fRequest1.equals(fRequest1));
        assertTrue("equals", fRequest2.equals(fRequest2));

        assertFalse("equals", fRequest1.equals(fRequest2));
        assertFalse("equals", fRequest2.equals(fRequest1));
	}

	public void testEqualsSymmetry() {
        assertTrue("equals", fRequest1.equals(fRequest1b));
        assertTrue("equals", fRequest1b.equals(fRequest1));

        assertFalse("equals", fRequest1.equals(fRequest3));
        assertFalse("equals", fRequest2.equals(fRequest3));
        assertFalse("equals", fRequest3.equals(fRequest1));
        assertFalse("equals", fRequest3.equals(fRequest2));
	}

	public void testEqualsTransivity() {
        assertTrue("equals", fRequest1.equals(fRequest1b));
        assertTrue("equals", fRequest1b.equals(fRequest1c));
        assertTrue("equals", fRequest1.equals(fRequest1c));
	}

	public void testEqualsNull() {
        assertFalse("equals", fRequest1.equals(null));
        assertFalse("equals", fRequest2.equals(null));
	}

	// ------------------------------------------------------------------------
	// hashCode
	// ------------------------------------------------------------------------

	public void testHashCode() {
        assertTrue("hashCode", fRequest1.hashCode() == fRequest1.hashCode());
        assertTrue("hashCode", fRequest2.hashCode() == fRequest2.hashCode());
		assertTrue("hashCode", fRequest1.hashCode() != fRequest2.hashCode());
	}

	// ------------------------------------------------------------------------
	// toString
	// ------------------------------------------------------------------------

	public void testToString() {
        String expected1 = "[TmfCoalescedDataRequest(0,ITmfEvent,10,100,200)]";
        String expected2 = "[TmfCoalescedDataRequest(1,ITmfEvent,20,100,200)]";
        String expected3 = "[TmfCoalescedDataRequest(2,ITmfEvent,20,200,200)]";
        String expected4 = "[TmfCoalescedDataRequest(3,ITmfEvent,20,200,300)]";

        assertEquals("toString", expected1, fRequest1.toString());
        assertEquals("toString", expected2, fRequest2.toString());
        assertEquals("toString", expected3, fRequest3.toString());
        assertEquals("toString", expected4, fRequest4.toString());
	}

	// ------------------------------------------------------------------------
	// isCompatible
	// ------------------------------------------------------------------------

	public void testIsCompatible() {
		TmfCoalescedDataRequest coalescedRequest = new TmfCoalescedDataRequest(TmfEvent.class, 10, 100, 200);
        TmfDataRequest request1 = new TmfDataRequestStub(TmfEvent.class,  10, 100, 200);
		TmfDataRequest request2 = new TmfDataRequestStub(TmfEvent.class,   5, 100, 200);
        TmfDataRequest request3 = new TmfDataRequestStub(TmfEvent.class,   5,   4, 200);
        TmfDataRequest request4 = new TmfDataRequestStub(TmfEvent.class,   5,   5, 200);
        TmfDataRequest request5 = new TmfDataRequestStub(TmfEvent.class,  15, 100, 200);
        TmfDataRequest request6 = new TmfDataRequestStub(TmfEvent.class, 100, 100, 200);
        TmfDataRequest request7 = new TmfDataRequestStub(TmfEvent.class, 110, 100, 200);
        TmfDataRequest request8 = new TmfDataRequestStub(TmfEvent.class, 111, 100, 200);

        assertTrue("isCompatible", coalescedRequest.isCompatible(request1));
        assertTrue("isCompatible", coalescedRequest.isCompatible(request2));
        assertTrue("isCompatible", coalescedRequest.isCompatible(request3));
        assertTrue("isCompatible", coalescedRequest.isCompatible(request4));
        assertTrue("isCompatible", coalescedRequest.isCompatible(request5));
        assertTrue("isCompatible", coalescedRequest.isCompatible(request6));
        assertTrue("isCompatible", coalescedRequest.isCompatible(request7));
        assertTrue("isCompatible", coalescedRequest.isCompatible(request8));

        TmfDataRequest request9  = new TmfDataRequestStub(TmfEvent.class,   5,   3, 200);
        TmfDataRequest request10 = new TmfDataRequestStub(TmfEvent.class, 112, 100, 200);

        assertFalse("isCompatible", coalescedRequest.isCompatible(request9));
        assertFalse("isCompatible", coalescedRequest.isCompatible(request10));
	}

    // ------------------------------------------------------------------------
    // addRequest
    // ------------------------------------------------------------------------

    public void testAddRequest1() {
        TmfCoalescedDataRequest coalescedRequest = new TmfCoalescedDataRequest(TmfEvent.class, 10, 100, 200);
        TmfDataRequest request = new TmfDataRequestStub(TmfEvent.class, 10, 100, 200);

        assertTrue("isCompatible", coalescedRequest.isCompatible(request));
        coalescedRequest.addRequest(request);
        assertEquals("addRequest",  10, coalescedRequest.getIndex());
        assertEquals("addRequest", 100, coalescedRequest.getNbRequested());
        assertEquals("addRequest", 200, coalescedRequest.getBlockSize());
    }

    public void testAddRequest2() {
        TmfCoalescedDataRequest coalescedRequest = new TmfCoalescedDataRequest(TmfEvent.class, 10, 100, 200);
        TmfDataRequest request = new TmfDataRequestStub(TmfEvent.class, 5, 100, 200);

        assertTrue("isCompatible", coalescedRequest.isCompatible(request));
        coalescedRequest.addRequest(request);
        assertEquals("addRequest",   5, coalescedRequest.getIndex());
        assertEquals("addRequest", 105, coalescedRequest.getNbRequested());
        assertEquals("addRequest", 200, coalescedRequest.getBlockSize());
    }

    public void testAddRequest3() {
        TmfCoalescedDataRequest coalescedRequest = new TmfCoalescedDataRequest(TmfEvent.class, 10, 100, 200);
        TmfDataRequest request = new TmfDataRequestStub(TmfEvent.class, 5, 4, 200);

        assertTrue("isCompatible", coalescedRequest.isCompatible(request));
        coalescedRequest.addRequest(request);
        assertEquals("addRequest",   5, coalescedRequest.getIndex());
        assertEquals("addRequest", 105, coalescedRequest.getNbRequested());
        assertEquals("addRequest", 200, coalescedRequest.getBlockSize());
    }

    public void testAddRequest4() {
        TmfCoalescedDataRequest coalescedRequest = new TmfCoalescedDataRequest(TmfEvent.class, 10, 100, 200);
        TmfDataRequest request = new TmfDataRequestStub(TmfEvent.class, 5, 5, 200);

        assertTrue("isCompatible", coalescedRequest.isCompatible(request));
        coalescedRequest.addRequest(request);
        assertEquals("addRequest",   5, coalescedRequest.getIndex());
        assertEquals("addRequest", 105, coalescedRequest.getNbRequested());
        assertEquals("addRequest", 200, coalescedRequest.getBlockSize());
    }

    public void testAddRequest5() {
        TmfCoalescedDataRequest coalescedRequest = new TmfCoalescedDataRequest(TmfEvent.class, 10, 100, 200);
        TmfDataRequest request = new TmfDataRequestStub(TmfEvent.class,  15, 100, 200);

        assertTrue("isCompatible", coalescedRequest.isCompatible(request));
        coalescedRequest.addRequest(request);
        assertEquals("addRequest",  10, coalescedRequest.getIndex());
        assertEquals("addRequest", 105, coalescedRequest.getNbRequested());
        assertEquals("addRequest", 200, coalescedRequest.getBlockSize());
    }

    public void testAddRequest6() {
        TmfCoalescedDataRequest coalescedRequest = new TmfCoalescedDataRequest(TmfEvent.class, 10, 100, 200);
        TmfDataRequest request = new TmfDataRequestStub(TmfEvent.class, 100, 100, 200);

        assertTrue("isCompatible", coalescedRequest.isCompatible(request));
        coalescedRequest.addRequest(request);
        assertEquals("addRequest",  10, coalescedRequest.getIndex());
        assertEquals("addRequest", 190, coalescedRequest.getNbRequested());
        assertEquals("addRequest", 200, coalescedRequest.getBlockSize());
    }

    public void testAddRequest7() {
        TmfCoalescedDataRequest coalescedRequest = new TmfCoalescedDataRequest(TmfEvent.class, 10, 100, 200);
        TmfDataRequest request = new TmfDataRequestStub(TmfEvent.class, 110, 100, 200);

        assertTrue("isCompatible", coalescedRequest.isCompatible(request));
        coalescedRequest.addRequest(request);
        assertEquals("addRequest",  10, coalescedRequest.getIndex());
        assertEquals("addRequest", 200, coalescedRequest.getNbRequested());
        assertEquals("addRequest", 200, coalescedRequest.getBlockSize());
    }

    public void testAddRequest8() {
        TmfCoalescedDataRequest coalescedRequest = new TmfCoalescedDataRequest(TmfEvent.class, 10, 100, 200);
        TmfDataRequest request = new TmfDataRequestStub(TmfEvent.class, 111, 100, 200);

        assertTrue("isCompatible", coalescedRequest.isCompatible(request));
        coalescedRequest.addRequest(request);
        assertEquals("addRequest",  10, coalescedRequest.getIndex());
        assertEquals("addRequest", 201, coalescedRequest.getNbRequested());
        assertEquals("addRequest", 200, coalescedRequest.getBlockSize());
    }

	// ------------------------------------------------------------------------
	// done
	// ------------------------------------------------------------------------

	public void testDone() {

		// Test request
		final boolean[] crFlags = new boolean[4];
		TmfCoalescedDataRequest request = setupTestRequest(crFlags);
		TmfDataRequest subRequest1 = new TmfDataRequestStub(TmfEvent.class, 10, 100, 200);
		TmfDataRequest subRequest2 = new TmfDataRequestStub(TmfEvent.class, 10, 100, 200);
		request.addRequest(subRequest1);
		request.addRequest(subRequest2);

		request.done();

		// Validate the coalescing request
		assertTrue ("isCompleted", request.isCompleted());
		assertFalse("isFailed",    request.isFailed());
		assertFalse("isCancelled", request.isCancelled());

		assertTrue ("handleCompleted", crFlags[0]);
		assertTrue ("handleSuccess",   crFlags[1]);
		assertFalse("handleFailure",   crFlags[2]);
		assertFalse("handleCancel",    crFlags[3]);

		// Validate the first coalesced request
		assertTrue ("isCompleted", subRequest1.isCompleted());
		assertFalse("isFailed",    subRequest1.isFailed());
		assertFalse("isCancelled", subRequest1.isCancelled());

		// Validate the second coalesced request
		assertTrue ("isCompleted", subRequest2.isCompleted());
		assertFalse("isFailed",    subRequest2.isFailed());
		assertFalse("isCancelled", subRequest2.isCancelled());
	}

	// ------------------------------------------------------------------------
	// fail
	// ------------------------------------------------------------------------

	public void testFail() {

		final boolean[] crFlags = new boolean[4];
		TmfCoalescedDataRequest request = setupTestRequest(crFlags);
		TmfDataRequest subRequest1 = new TmfDataRequestStub(TmfEvent.class, 10, 100, 200);
		TmfDataRequest subRequest2 = new TmfDataRequestStub(TmfEvent.class, 10, 100, 200);
		request.addRequest(subRequest1);
		request.addRequest(subRequest2);

		request.fail();

		// Validate the coalescing request
		assertTrue ("isCompleted", request.isCompleted());
		assertTrue ("isFailed",    request.isFailed());
		assertFalse("isCancelled", request.isCancelled());

		assertTrue ("handleCompleted", crFlags[0]);
		assertFalse("handleSuccess",   crFlags[1]);
		assertTrue ("handleFailure",   crFlags[2]);
		assertFalse("handleCancel",    crFlags[3]);

		// Validate the first coalesced request
		assertTrue ("isCompleted", subRequest1.isCompleted());
		assertTrue ("isFailed",    subRequest1.isFailed());
		assertFalse("isCancelled", subRequest1.isCancelled());

		// Validate the second coalesced request
		assertTrue ("isCompleted", subRequest2.isCompleted());
		assertTrue ("isFailed",    subRequest2.isFailed());
		assertFalse("isCancelled", subRequest2.isCancelled());
	}

	// ------------------------------------------------------------------------
	// cancel
	// ------------------------------------------------------------------------

	public void testCancel() {

		final boolean[] crFlags = new boolean[4];
		TmfCoalescedDataRequest request = setupTestRequest(crFlags);
		TmfDataRequest subRequest1 = new TmfDataRequestStub(TmfEvent.class, 10, 100, 200);
		TmfDataRequest subRequest2 = new TmfDataRequestStub(TmfEvent.class, 10, 100, 200);
		request.addRequest(subRequest1);
		request.addRequest(subRequest2);

		request.cancel();

		// Validate the coalescing request
		assertTrue ("isCompleted", request.isCompleted());
		assertFalse("isFailed",    request.isFailed());
		assertTrue ("isCancelled", request.isCancelled());

		assertTrue ("handleCompleted", crFlags[0]);
		assertFalse("handleSuccess",   crFlags[1]);
		assertFalse("handleFailure",   crFlags[2]);
		assertTrue ("handleCancel",    crFlags[3]);

		// Validate the first coalesced request
		assertTrue ("isCompleted", subRequest1.isCompleted());
		assertFalse("isFailed",    subRequest1.isFailed());
		assertTrue ("isCancelled", subRequest1.isCancelled());

		// Validate the second coalesced request
		assertTrue ("isCompleted", subRequest2.isCompleted());
		assertFalse("isFailed",    subRequest2.isFailed());
		assertTrue ("isCancelled", subRequest2.isCancelled());
	}


	// ------------------------------------------------------------------------
	// cancel sub-requests
	// ------------------------------------------------------------------------

    public void testCancelSubRequests() {

	        final boolean[] crFlags = new boolean[4];
	        TmfCoalescedDataRequest request = setupTestRequest(crFlags);
	        TmfDataRequest subRequest1 = new TmfDataRequestStub(TmfEvent.class, 10, 100, 200);
	        TmfDataRequest subRequest2 = new TmfDataRequestStub(TmfEvent.class, 10, 100, 200);
	        request.addRequest(subRequest1);
	        request.addRequest(subRequest2);

	        subRequest1.cancel();

	        // Validate the first coalesced request
	        assertTrue ("isCompleted", subRequest1.isCompleted());
	        assertFalse("isFailed",    subRequest1.isFailed());
	        assertTrue ("isCancelled", subRequest1.isCancelled());

	        // Validate the coalescing request
	        assertFalse("isCompleted", request.isCompleted());
	        assertFalse("isFailed",     request.isFailed());
	        assertFalse("isCancelled", request.isCancelled());

	        // Cancel second sub-request
	        subRequest2.cancel();

	        // Validate the second coalesced request
	        assertTrue ("isCompleted", subRequest2.isCompleted());
	        assertFalse("isFailed",    subRequest2.isFailed());
	        assertTrue ("isCancelled", subRequest2.isCancelled());

	        // Validate the coalescing request
	        assertTrue ("isCompleted", request.isCompleted());
	        assertFalse("isFailed",    request.isFailed());
	        assertTrue ("isCancelled", request.isCancelled());

	        // Finalize coalescing request -
	        // Note: No need to check "request.isCancelled()" since it was verified above
            request.cancel();

	        assertTrue ("handleCompleted", crFlags[0]);
	        assertFalse("handleSuccess",   crFlags[1]);
	        assertFalse("handleFailure",   crFlags[2]);
	        assertTrue ("handleCancel",    crFlags[3]);
	    }

}