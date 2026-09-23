package `is`.xyz.mpv

import android.net.Uri
import org.mockito.Mockito

object DummyUri {
    fun dummy(): Uri = Mockito.mock(Uri::class.java)
}
