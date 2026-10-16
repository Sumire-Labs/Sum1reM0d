using System;
public static class TextureExport {
 public static int[] Bounds(byte[] bytes,int width,int height,int stride){
  int minX=width,minY=height,maxX=-1,maxY=-1;
  for(int y=0;y<height;y++)for(int x=0;x<width;x++)if(bytes[y*stride+x*4+3]>=128){minX=Math.Min(minX,x);minY=Math.Min(minY,y);maxX=Math.Max(maxX,x);maxY=Math.Max(maxY,y);}
  return new[]{minX,minY,maxX,maxY};
 }
 // Deterministic, non-dithered palette normalization for native 16x16 exports.
 public static void Quantize(byte[] data,int width,int height,int stride,int limit){
  var points=new System.Collections.Generic.List<double[]>();var offsets=new System.Collections.Generic.List<int>();
  for(int y=0;y<height;y++)for(int x=0;x<width;x++){int i=y*stride+x*4;if(data[i+3]>0){points.Add(new double[]{data[i],data[i+1],data[i+2]});offsets.Add(i);}else data[i]=data[i+1]=data[i+2]=0;}
  if(points.Count==0)return;var centers=new System.Collections.Generic.List<double[]>();centers.Add((double[])points[0].Clone());
  while(centers.Count<limit){double far=-1;double[] chosen=null;foreach(var p in points){double d=double.MaxValue;foreach(var c in centers)d=Math.Min(d,Distance(p,c));if(d>far){far=d;chosen=p;}}if(far<64)break;centers.Add((double[])chosen.Clone());}
  for(int iteration=0;iteration<12;iteration++){var sums=new double[centers.Count,3];var counts=new int[centers.Count];foreach(var p in points){int best=Nearest(p,centers);counts[best]++;for(int c=0;c<3;c++)sums[best,c]+=p[c];}for(int k=0;k<centers.Count;k++)if(counts[k]>0)for(int c=0;c<3;c++)centers[k][c]=sums[k,c]/counts[k];}
  for(int p=0;p<points.Count;p++){var color=centers[Nearest(points[p],centers)];for(int c=0;c<3;c++)data[offsets[p]+c]=(byte)Math.Round(color[c]);}
 }
 private static double Distance(double[] a,double[] b){double d=0;for(int i=0;i<3;i++){double q=a[i]-b[i];d+=q*q;}return d;}
 private static int Nearest(double[] p,System.Collections.Generic.List<double[]> centers){int best=0;double distance=double.MaxValue;for(int k=0;k<centers.Count;k++){double d=Distance(p,centers[k]);if(d<distance){best=k;distance=d;}}return best;}
 public static int[] Palette(byte[] bytes,int width,int height,int stride,string family){
  var colors=new System.Collections.Generic.List<int[]>();
  for(int y=0;y<height;y+=3)for(int x=0;x<width;x+=3){int i=y*stride+x*4;if(bytes[i+3]<128)continue;int b=bytes[i],g=bytes[i+1],r=bytes[i+2];bool material=family=="emerald"?g>r*1.12&&g>b*1.05:family=="redstone"?r>g*1.15&&r>b*1.12:family=="lapis"?b>r*1.15&&b>g*1.05:r>g*1.1&&b>g*1.15;if(material)colors.Add(new[]{r,g,b});}
  if(colors.Count<10)throw new ArgumentException("Generated material palette is missing: "+family);
  colors.Sort((a,b)=>Light(a).CompareTo(Light(b)));var result=new int[9];int[] indices={colors.Count/20,colors.Count/2,colors.Count*19/20};for(int k=0;k<3;k++)for(int c=0;c<3;c++)result[k*3+c]=colors[indices[k]][c];return result;
 }
 private static double Light(int[] c){return c[0]*.2126+c[1]*.7152+c[2]*.0722;}
 private static bool Wood(int r,int g,int b){return r>g*1.2 && g>b*1.2;}
 public static void Material(byte[] bytes,int width,int height,int stride,int[] palette,bool preserveWood){
  // Keep the native value/shading exactly; only the material hue/saturation changes.
  int peak=Math.Max(palette[3],Math.Max(palette[4],palette[5]));if(peak==0)throw new ArgumentException("Empty material palette");
  for(int y=0;y<height;y++)for(int x=0;x<width;x++){int i=y*stride+x*4;int b=bytes[i],g=bytes[i+1],r=bytes[i+2];if(bytes[i+3]==0 || preserveWood&&Wood(r,g,b))continue;int value=Math.Max(r,Math.Max(g,b));for(int c=0;c<3;c++)bytes[i+2-c]=(byte)Math.Round(value*(double)palette[3+c]/peak);}
 }
}
