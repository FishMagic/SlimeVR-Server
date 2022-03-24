package me.ftmc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import dev.slimevr.vr.processor.TransformNode
import kotlinx.coroutines.delay


@Composable
fun SkeletonPage(nodeList: Array<TransformNode>) {
  Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Row(
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(text = "Skeleton", style = MaterialTheme.typography.h5)
    }
    Spacer(Modifier.height(4.dp))
    Row(modifier = Modifier.fillMaxHeight(), verticalAlignment = Alignment.Top) {
      SkeletonList(nodeList)
    }
  }
}

@Composable
private fun SkeletonList(nodeList: Array<TransformNode>) {
  Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
      Column(
        modifier = Modifier.weight(.4f),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
      ) {
        Text(text = "Joint")
      }
      Spacer(modifier = Modifier.width(4.dp))
      Column(
        modifier = Modifier.weight(.1f),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(text = "X")
      }
      Spacer(modifier = Modifier.width(4.dp))
      Column(
        modifier = Modifier.weight(.1f),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(text = "Y")
      }
      Spacer(modifier = Modifier.width(4.dp))
      Column(
        modifier = Modifier.weight(.1f),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(text = "Z")
      }
      Spacer(modifier = Modifier.width(4.dp))
      Column(
        modifier = Modifier.weight(.1f),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(text = "Pitch")
      }
      Spacer(modifier = Modifier.width(4.dp))
      Column(
        modifier = Modifier.weight(.1f),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(text = "Yaw")
      }
      Spacer(modifier = Modifier.width(4.dp))
      Column(
        modifier = Modifier.weight(.1f),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(text = "Roll")
      }
    }
    Divider(modifier = Modifier.fillMaxWidth())
    Row(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.fillMaxWidth()) {
        for (node in nodeList) {
          Row(modifier = Modifier.fillMaxWidth()) {
            var x by remember { mutableStateOf("") }
            var y by remember { mutableStateOf("") }
            var z by remember { mutableStateOf("") }
            var a1 by remember { mutableStateOf("") }
            var a2 by remember { mutableStateOf("") }
            var a3 by remember { mutableStateOf("") }
            Column(modifier = Modifier.weight(.4f), horizontalAlignment = Alignment.End) {
              Text(text = node.name)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(.1f), horizontalAlignment = Alignment.CenterHorizontally) {
              Text(text = x)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(.1f), horizontalAlignment = Alignment.CenterHorizontally) {
              Text(text = y)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(.1f), horizontalAlignment = Alignment.CenterHorizontally) {
              Text(text = z)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(.1f), horizontalAlignment = Alignment.CenterHorizontally) {
              Text(text = a1)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(.1f), horizontalAlignment = Alignment.CenterHorizontally) {
              Text(text = a2)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(.1f), horizontalAlignment = Alignment.CenterHorizontally) {
              Text(text = a3)
            }
            LaunchedEffect(true) {
              while (true) {
                val v = Vector3f()
                val q = Quaternion()
                val angles = FloatArray(3)
                node.worldTransform.getTranslation(v)
                node.worldTransform.getRotation(q)
                q.toAngles(angles)
                x = String.format("%.2f", v.x)
                y = String.format("%.2f", v.y)
                z = String.format("%.2f", v.z)
                a1 = String.format("%.0f", angles[0] * FastMath.RAD_TO_DEG)
                a2 = String.format("%.0f", angles[1] * FastMath.RAD_TO_DEG)
                a3 = String.format("%.0f", angles[2] * FastMath.RAD_TO_DEG)
                delay(500L)
              }
            }
          }
          Divider(modifier = Modifier.fillMaxWidth())
        }
      }
    }
  }
}