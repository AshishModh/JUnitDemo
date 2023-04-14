package com.liquidnet.pst.helm.wizard.logdownload;

import com.liquidnet.gpss.lib.app.GpssApplicationException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import spark.Request;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@RunWith(MockitoJUnitRunner.class)
public class    LogDownloadRequestTest {

    @Test(expected = GpssApplicationException.class)
    public void inValidLogDownloadRequestActionType() throws GpssApplicationException {
        Request request = mock(Request.class);
        Mockito.when(request.params("memberid")).thenReturn(null);
        Mockito.when(request.params("userid")).thenReturn(null);
        Mockito.when(request.params("filename")).thenReturn(null);
        LogDownloadRequest classToTest = new LogDownloadRequest(request, "XYZ");
        classToTest.validateRequestActionType();
    }

    @Test
    public void validLogDownloadRequestActionType() throws GpssApplicationException {
        Request request = mock(Request.class);
        Mockito.when(request.params("memberid")).thenReturn(null);
        Mockito.when(request.params("userid")).thenReturn(null);
        Mockito.when(request.params("filename")).thenReturn(null);
        LogDownloadRequest classToTest = new LogDownloadRequest(request, "DIR");
        classToTest = spy(classToTest);
        classToTest.validateRequestActionType();
        Mockito.verify(classToTest, Mockito.timeout(3000).times(1)).validateRequestActionType();
    }

    @Test
    public void logDownloadRequestGetter() {
        Request request = mock(Request.class);
        Mockito.when(request.params("memberid")).thenReturn("USER");
        Mockito.when(request.params("userid")).thenReturn("USER1");
        Mockito.when(request.params("filename")).thenReturn("FILENAME");
        LogDownloadRequest classToTest = new LogDownloadRequest(request, "DIR");
        Assert.assertEquals("USER", classToTest.getMemberId());
        Assert.assertEquals("USER1", classToTest.getUserId());
        Assert.assertEquals("FILENAME", classToTest.getFileName());
        Assert.assertEquals("DIR", classToTest.getActionType());
        Assert.assertEquals(false, classToTest.getIsOMSI());
        Assert.assertEquals("USERUSER1DIR", classToTest.getRequestKey());
        Assert.assertEquals("RemoteComponent_Traders_USER_USER1", classToTest.getTopic());
    }

    @Test
    public void logDownloadRequestGetterIsOMSI() {
        Request request = mock(Request.class);
        Mockito.when(request.params("memberid")).thenReturn("USER");
        Mockito.when(request.params("userid")).thenReturn("OMSI");
        Mockito.when(request.params("filename")).thenReturn("FILENAME");
        LogDownloadRequest classToTest = new LogDownloadRequest(request, "DIR");
        Assert.assertEquals("USER", classToTest.getMemberId());
        Assert.assertEquals("", classToTest.getUserId());
        Assert.assertEquals("FILENAME", classToTest.getFileName());
        Assert.assertEquals("DIR", classToTest.getActionType());
        Assert.assertEquals(true, classToTest.getIsOMSI());
        Assert.assertEquals("USERDIR", classToTest.getRequestKey());
        Assert.assertEquals("RemoteComponent_Butler_USER", classToTest.getTopic());
    }

    @Test
    public void logDownloadRequestWithSpaceGetter() {
        Request request = mock(Request.class);
        Mockito.when(request.params("memberid")).thenReturn(" U SE R ");
        Mockito.when(request.params("userid")).thenReturn("USER 1");
        Mockito.when(request.params("filename")).thenReturn("FILE NAME");
        LogDownloadRequest classToTest = new LogDownloadRequest(request, "DIR");
        Assert.assertEquals("U SE R", classToTest.getMemberId());
        Assert.assertEquals("USER 1", classToTest.getUserId());
        Assert.assertEquals("FILE NAME", classToTest.getFileName());
        Assert.assertEquals("DIR", classToTest.getActionType());
        Assert.assertEquals(false, classToTest.getIsOMSI());
        Assert.assertEquals("USERUSER1DIR", classToTest.getRequestKey());
        Assert.assertEquals("RemoteComponent_Traders_USER_USER1", classToTest.getTopic());
    }

    @Test
    public void logDownloadRequestWithSpaceGetterIsOMSI() {
        Request request = mock(Request.class);
        Mockito.when(request.params("memberid")).thenReturn("U SE R");
        Mockito.when(request.params("userid")).thenReturn("");
        Mockito.when(request.params("filename")).thenReturn("FILE NAME");
        LogDownloadRequest classToTest = new LogDownloadRequest(request, "DIR");
        Assert.assertEquals("U SE R", classToTest.getMemberId());
        Assert.assertEquals("", classToTest.getUserId());
        Assert.assertEquals("FILE NAME", classToTest.getFileName());
        Assert.assertEquals("DIR", classToTest.getActionType());
        Assert.assertEquals(true, classToTest.getIsOMSI());
        Assert.assertEquals("USERDIR", classToTest.getRequestKey());
        Assert.assertEquals("RemoteComponent_Butler_USER", classToTest.getTopic());
    }

}
